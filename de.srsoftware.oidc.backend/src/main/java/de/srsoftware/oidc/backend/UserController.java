/* © SRSoftware 2024 */
package de.srsoftware.oidc.backend;

import static de.srsoftware.oidc.api.User.*;
import static java.net.HttpURLConnection.*;

import com.sun.net.httpserver.HttpExchange;
import de.srsoftware.cookies.SessionToken;
import de.srsoftware.oidc.api.*;
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

	private boolean userInfo(HttpExchange ex) throws IOException {
		var optUser = getBearer(ex).flatMap(users::forToken);
		if (optUser.isEmpty()) return sendEmptyResponse(HTTP_UNAUTHORIZED, ex);
		var user = optUser.get();
		var map  = Map.of("sub", user.uuid(), "email", user.email());
		return sendContent(ex, new JSONObject(map));
	}


	@Override
	public boolean doPost(String path, HttpExchange ex) throws IOException {
		switch (path) {
			case "/login":
				return login(ex);
		}
		var optSession = getSession(ex);
		if (optSession.isEmpty()) return sendEmptyResponse(HTTP_UNAUTHORIZED, ex);

		// post-login paths
		var session = optSession.get();
		switch (path) {
			case "/":
				return sendUserAndCookie(ex, session);
			case "/password":
				return updatePassword(ex, session);
			case "/update":
				return updateUser(ex, session);
		}
		return notFound(ex);
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
		users.save(user);
		return sendContent(ex, user.map(false));
	}
}
