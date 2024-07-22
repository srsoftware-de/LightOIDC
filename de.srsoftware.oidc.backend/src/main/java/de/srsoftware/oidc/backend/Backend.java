/* © SRSoftware 2024 */
package de.srsoftware.oidc.backend;

import static de.srsoftware.oidc.api.Constants.*;
import static de.srsoftware.oidc.api.Permission.MANAGE_CLIENTS;
import static de.srsoftware.oidc.api.User.*;
import static java.net.HttpURLConnection.*;

import com.sun.net.httpserver.HttpExchange;
import de.srsoftware.cookies.SessionToken;
import de.srsoftware.oidc.api.*;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import org.json.JSONObject;

public class Backend extends PathHandler {
	private final SessionService sessions;
	private final UserService    users;
	private final ClientService  clients;

	public Backend(ClientService clientService, SessionService sessionService, UserService userService) {
		clients  = clientService;
		sessions = sessionService;
		users    = userService;
	}

	private boolean addClient(HttpExchange ex, Session session) throws IOException {
		if (!session.user().hasPermission(MANAGE_CLIENTS)) return sendError(ex, "NOT ALLOWED");
		var json      = json(ex);
		var redirects = new HashSet<String>();
		for (Object o : json.getJSONArray(REDIRECT_URIS)) {
			if (o instanceof String s) redirects.add(s);
		}
		var client = new Client(json.getString(CLIENT_ID), json.getString(NAME), json.getString(SECRET), redirects);
		clients.add(client);
		return sendContent(ex, client);
	}

	private boolean authorize(HttpExchange ex, Session session) throws IOException {
		var json     = json(ex);
		var clientId = json.getString(CLIENT_ID);
		var redirect = json.getString(REDIRECT_URI);
		System.out.println(json);
		return sendEmptyResponse(HTTP_NOT_FOUND, ex);
	}

	private boolean clients(HttpExchange ex, Session session) throws IOException {
		var user = session.user();
		if (!user.hasPermission(MANAGE_CLIENTS)) return sendEmptyResponse(HTTP_FORBIDDEN, ex);
		var json = new JSONObject();
		clients.listClients().forEach(client -> json.put(client.id(), Map.of("name", client.name(), "redirect_uris", client.redirectUris())));
		return sendContent(ex, json);
	}

	private boolean deleteClient(HttpExchange ex, Session session) throws IOException {
		if (!session.user().hasPermission(MANAGE_CLIENTS)) return sendError(ex, "NOT ALLOWED");
		var json = json(ex);
		var id   = json.getString(CLIENT_ID);
		clients.getClient(id).ifPresent(clients::remove);
		return sendEmptyResponse(HTTP_OK, ex);
	}

	private boolean doLogin(HttpExchange ex) throws IOException {
		var body = json(ex);

		var username = body.has(USERNAME) ? body.getString(USERNAME) : null;
		var password = body.has(PASSWORD) ? body.getString(PASSWORD) : null;

		Optional<User> user = users.load(username, password);
		if (user.isPresent()) return sendUserAndCookie(ex, sessions.createSession(user.get()));
		return sendEmptyResponse(HTTP_UNAUTHORIZED, ex);
	}

	@Override
	public boolean doDelete(String path, HttpExchange ex) throws IOException {
		var optSession = getSession(ex);
		if (optSession.isEmpty()) return sendEmptyResponse(HTTP_UNAUTHORIZED, ex);

		// post-login paths
		var session = optSession.get();
		switch (path) {
			case "/client":
				return deleteClient(ex, session);
		}

		System.err.println("not implemented");
		return sendEmptyResponse(HTTP_NOT_FOUND, ex);
	}

	@Override
	public boolean doGet(String path, HttpExchange ex) throws IOException {
		// pre-login paths
		switch (path) {
			case "/openid-configuration":
				return openidConfig(ex);
		}

		var optSession = getSession(ex);
		if (optSession.isEmpty()) return sendEmptyResponse(HTTP_UNAUTHORIZED, ex);

		// post-login paths
		var session = optSession.get();
		switch (path) {
			case "/logout":
				return logout(ex, session);
		}

		System.err.println("not implemented");
		return sendEmptyResponse(HTTP_NOT_FOUND, ex);
	}

	@Override
	public boolean doPost(String path, HttpExchange ex) throws IOException {
		// pre-login paths
		switch (path) {
			case "/login":
				return doLogin(ex);
		}
		var optSession = getSession(ex);
		if (optSession.isEmpty()) return sendEmptyResponse(HTTP_UNAUTHORIZED, ex);

		// post-login paths
		var session = optSession.get();
		switch (path) {
			case "/add/client":
				return addClient(ex, session);
			case "/authorize":
				return authorize(ex, session);
			case "/client":
				return loadClient(ex, session);
			case "/clients":
				return clients(ex, session);
			case "/update/password":
				return updatePassword(ex, session);
			case "/update/user":
				return updateUser(ex, session);
			case "/user":
				return sendUserAndCookie(ex, session);
		}
		System.err.println("not implemented");
		return sendEmptyResponse(HTTP_NOT_FOUND, ex);
	}

	private Optional<Session> getSession(HttpExchange ex) {
		return SessionToken.from(ex).map(SessionToken::sessionId).flatMap(sessions::retrieve);
	}

	private boolean loadClient(HttpExchange ex, Session session) throws IOException {
		if (!session.user().hasPermission(MANAGE_CLIENTS)) return sendEmptyResponse(HTTP_FORBIDDEN, ex);
		var json = json(ex);
		if (json.has(CLIENT_ID)) {
			var clientID = json.getString(CLIENT_ID);
			var client   = clients.getClient(clientID).map(Client::map).map(JSONObject::new);
			if (client.isPresent()) return sendContent(ex, client.get());
		}
		return sendEmptyResponse(HTTP_NOT_FOUND, ex);
	}

	private boolean logout(HttpExchange ex, Session session) throws IOException {
		sessions.dropSession(session.id());
		new SessionToken("").addTo(ex);
		return sendEmptyResponse(HTTP_OK, ex);
	}

	private boolean openidConfig(HttpExchange ex) throws IOException {
		var        uri	= ex.getRequestURI().toString();
		JSONObject json = new JSONObject();

		json.put("authorization_endpoint", prefix(ex) + "/web/authorization.html");
		return sendContent(ex, json);
	}


	private boolean sendUserAndCookie(HttpExchange ex, Session session) throws IOException {
		new SessionToken(session.id()).addTo(ex);
		return sendContent(ex, new JSONObject(session.user().map(false)));
	}

	private boolean updatePassword(HttpExchange ex, Session session) throws IOException {
		var user = session.user();
		var json = json(ex);
		var uuid = json.getString(UUID);
		if (!uuid.equals(user.uuid())) {
			return sendEmptyResponse(HTTP_FORBIDDEN, ex);
		}
		var oldPass = json.getString("oldpass");
		if (!users.passwordMatches(oldPass, user.hashedPassword())) return sendError(ex, "wrong password");

		var newpass  = json.getJSONArray("newpass");
		var newPass1 = newpass.getString(0);
		if (!newPass1.equals(newpass.getString(1))) {
			return sendError(ex, "password mismatch");
		}
		users.updatePassword(user, newPass1);
		return sendContent(ex, new JSONObject(user.map(false)));
	}

	private boolean updateUser(HttpExchange ex, Session session) throws IOException {
		var user = session.user();
		var json = json(ex);
		var uuid = json.getString(UUID);
		if (!uuid.equals(user.uuid())) {
			return sendEmptyResponse(HTTP_FORBIDDEN, ex);
		}
		user.username(json.getString(USERNAME));
		user.email(json.getString(EMAIL));
		users.save(user);
		JSONObject response = new JSONObject(user.map(false));
		return sendContent(ex, response);
	}
}
