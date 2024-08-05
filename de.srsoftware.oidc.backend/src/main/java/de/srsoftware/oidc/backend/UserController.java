/* © SRSoftware 2024 */
package de.srsoftware.oidc.backend;

import static de.srsoftware.oidc.api.data.Permission.MANAGE_USERS;
import static de.srsoftware.oidc.api.data.User.*;
import static de.srsoftware.utils.Strings.uuid;
import static java.lang.System.Logger.Level.WARNING;
import static java.net.HttpURLConnection.*;

import com.sun.net.httpserver.HttpExchange;
import de.srsoftware.http.SessionToken;
import de.srsoftware.oidc.api.*;
import de.srsoftware.oidc.api.data.Session;
import de.srsoftware.oidc.api.data.User;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import org.json.JSONObject;

public class UserController extends Controller {
	private final UserService users;

	public UserController(SessionService sessionService, UserService userService) {
		super(sessionService);
		users = userService;
	}

	private boolean addUser(HttpExchange ex, Session session) throws IOException {
		var user = session.user();
		if (!user.hasPermission(MANAGE_USERS)) return sendEmptyResponse(HTTP_FORBIDDEN, ex);
		var json  = json(ex);
		var newID = uuid();
		User.of(json, uuid()).ifPresent(u -> users.updatePassword(u, json.getString(PASSWORD)));
		return sendContent(ex, newID);
	}

	@Override
	public boolean doGet(String path, HttpExchange ex) throws IOException {
		switch (path) {
			case "/info":
				return userInfo(ex);
		}
		var optSession = getSession(ex);
		if (optSession.isEmpty()) return sendEmptyResponse(HTTP_UNAUTHORIZED, ex);

		// post-login paths
		var session = optSession.get();
		switch (path) {
			case "/logout":
				return logout(ex, session);
		}

		return notFound(ex);
	}


	@Override
	public boolean doPost(String path, HttpExchange ex) throws IOException {
		switch (path) {
			case "/login":
				return login(ex);
			case "/reset":
				return resetPassword(ex);
		}
		var optSession = getSession(ex);
		if (optSession.isEmpty()) return sendEmptyResponse(HTTP_UNAUTHORIZED, ex);

		// post-login paths
		var session = optSession.get();
		switch (path) {
			case "/":
				return sendUserAndCookie(ex, session);
			case "/add":
				return addUser(ex, session);
			case "/list":
				return list(ex, session);
			case "/password":
				return updatePassword(ex, session);
			case "/update":
				return updateUser(ex, session);
		}
		return notFound(ex);
	}

	private boolean list(HttpExchange ex, Session session) throws IOException {
		var user = session.user();
		if (!user.hasPermission(MANAGE_USERS)) return sendEmptyResponse(HTTP_FORBIDDEN, ex);
		var json = new JSONObject();
		users.list().forEach(u -> json.put(u.uuid(), u.map(false)));
		return sendContent(ex, json);
	}

	private boolean login(HttpExchange ex) throws IOException {
		var body = json(ex);

		var username = body.has(USERNAME) ? body.getString(USERNAME) : null;
		var password = body.has(PASSWORD) ? body.getString(PASSWORD) : null;

		Optional<User> user = users.load(username, password);
		if (user.isPresent()) return sendUserAndCookie(ex, sessions.createSession(user.get()));
		return sendEmptyResponse(HTTP_UNAUTHORIZED, ex);
	}

	private boolean logout(HttpExchange ex, Session session) throws IOException {
		sessions.dropSession(session.id());
		new SessionToken("").addTo(ex);
		return sendEmptyResponse(HTTP_OK, ex);
	}

	private boolean resetPassword(HttpExchange ex) throws IOException {
		var idOrEmail = body(ex);
		users.find(idOrEmail).forEach(this::senPasswordLink);
		return sendEmptyResponse(HTTP_OK, ex);
	}

	private void senPasswordLink(User user) {
		LOG.log(WARNING, "Sending password link to {0}", user.email());
	}

	private boolean sendUserAndCookie(HttpExchange ex, Session session) throws IOException {
		new SessionToken(session.id()).addTo(ex);
		return sendContent(ex, session.user().map(false));
	}

	private boolean updatePassword(HttpExchange ex, Session session) throws IOException {
		var user = session.user();
		var json = json(ex);
		var uuid = json.getString(UUID);
		if (!uuid.equals(user.uuid())) {
			return sendEmptyResponse(HTTP_FORBIDDEN, ex);
		}
		var oldPass = json.getString("oldpass");
		if (!users.passwordMatches(oldPass, user.hashedPassword())) return badRequest(ex, "wrong password");

		var newpass  = json.getJSONArray("newpass");
		var newPass1 = newpass.getString(0);
		if (!newPass1.equals(newpass.getString(1))) {
			return badRequest(ex, "password mismatch");
		}
		users.updatePassword(user, newPass1);
		return sendContent(ex, user.map(false));
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
		user.realName(json.getString(REALNAME));
		users.save(user);
		return sendContent(ex, user.map(false));
	}

	private boolean userInfo(HttpExchange ex) throws IOException {
		var optUser = getBearer(ex).flatMap(users::forToken);
		if (optUser.isEmpty()) return sendEmptyResponse(HTTP_UNAUTHORIZED, ex);
		var user = optUser.get();
		var map  = Map.of("sub", user.uuid(), "email", user.email());
		return sendContent(ex, new JSONObject(map));
	}
}
