/* © SRSoftware 2024 */
package de.srsoftware.oidc.backend;

import static de.srsoftware.oidc.api.Permission.MANAGE_CLIENTS;
import static de.srsoftware.oidc.api.User.*;
import static java.net.HttpURLConnection.*;
import static java.nio.charset.StandardCharsets.UTF_8;

import com.sun.net.httpserver.HttpExchange;
import de.srsoftware.cookies.SessionToken;
import de.srsoftware.oidc.api.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;

import org.json.JSONArray;
import org.json.JSONObject;

public class Backend extends PathHandler {
	private static final String CLIENT_ID = "client_id";
	private static final String REDIRECT_URI = "redirect_uri";
	private final SessionService sessions;
	private final UserService    users;
	private final ClientService clients;

	public Backend(ClientService clientService, SessionService sessionService, UserService userService) {
		clients = clientService;
		sessions = sessionService;
		users    = userService;
	}

	private boolean authorize(HttpExchange ex, Session session) throws IOException {
		var json = json(ex);
		var clientId = json.getString(CLIENT_ID);
		var redirect = json.getString(REDIRECT_URI);
		System.out.println(json);
		return sendEmptyResponse(HTTP_NOT_FOUND,ex);
	}

	private boolean clients(HttpExchange ex, Session session) throws IOException {
		var user = session.user();
		if (!user.hasPermission(MANAGE_CLIENTS)) return sendEmptyResponse(HTTP_FORBIDDEN,ex);
		var json = new JSONObject();
		clients.listClients().forEach(client -> json.put(client.id(), Map.of("name",client.name(),"redirect_uris",client.redirectUris())));
		return sendContent(ex,json);
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
	public boolean doGet(String path, HttpExchange ex) throws IOException {
		switch (path) {
			case "/openid-configuration":
				return openidConfig(ex);
		}
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
			case "/authorize":
				return authorize(ex,session);
			case "/clients":
				return clients(ex,session);
			case "/update/password":
					return updatePassword(ex,session);
			case "/update/user":
				return updateUser(ex,session);
			case "/user":
				return sendUserAndCookie(ex, session);
		}
		System.err.println("not implemented");
		return sendEmptyResponse(HTTP_NOT_FOUND, ex);
	}

	private Optional<Session> getSession(HttpExchange ex) {
		return SessionToken.from(ex).map(SessionToken::sessionId).flatMap(sessions::retrieve);
	}

	private boolean openidConfig(HttpExchange ex) throws IOException {
		var        uri	= ex.getRequestURI().toString();
		JSONObject json = new JSONObject();

		json.put("authorization_endpoint", prefix(ex) + "/web/authorization.html");
		return sendContent(ex, json);
	}


	private boolean sendUserAndCookie(HttpExchange ex, Session session) throws IOException {
		var bytes   = new JSONObject(session.user().map(false)).toString().getBytes(UTF_8);
		var headers = ex.getResponseHeaders();

		headers.add(CONTENT_TYPE, JSON);
		new SessionToken(session.id()).addTo(headers);
		ex.sendResponseHeaders(200, bytes.length);
		var out = ex.getResponseBody();
		out.write(bytes);
		return true;
	}

	private boolean updatePassword(HttpExchange ex, Session session) throws IOException {
		var user =session.user();
		var json = json(ex);
		var uuid = json.getString(UUID);
		if (!uuid.equals(user.uuid())) {
			return sendEmptyResponse(HTTP_FORBIDDEN, ex);
		}
		var oldPass = json.getJSONArray("oldpass");
		var oldPass1 = oldPass.getString(0);
		if (!oldPass1.equals(oldPass.getString(1))){
			return sendError(ex,"password mismatch");
		}
		if (!users.passwordMatches(oldPass1,user.hashedPassword())) return sendError(ex,"wrong password");
		users.updatePassword(user,json.getString("newpass"));
		return sendContent(ex,new JSONObject(user.map(false)));
	}

	private boolean updateUser(HttpExchange ex, Session session) throws IOException {
		var user =session.user();
		var json = json(ex);
		var uuid = json.getString(UUID);
		if (!uuid.equals(user.uuid())){
			return sendEmptyResponse(HTTP_FORBIDDEN,ex);
		}
		user.username(json.getString(USERNAME));
		user.email(json.getString(EMAIL));
		users.save(user);
		JSONObject response = new JSONObject(user.map(false));
		return sendContent(ex,response);
	}
}
