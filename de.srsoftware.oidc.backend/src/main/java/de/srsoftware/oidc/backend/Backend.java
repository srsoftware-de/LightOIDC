/* © SRSoftware 2024 */
package de.srsoftware.oidc.backend;

import static de.srsoftware.oidc.api.User.PASSWORD;
import static de.srsoftware.oidc.api.User.USERNAME;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static java.net.HttpURLConnection.HTTP_UNAUTHORIZED;
import static java.nio.charset.StandardCharsets.UTF_8;

import com.sun.net.httpserver.HttpExchange;
import de.srsoftware.cookies.SessionToken;
import de.srsoftware.oidc.api.*;
import java.io.IOException;
import java.util.Optional;
import org.json.JSONObject;

public class Backend extends PathHandler {
	private final SessionService sessions;
	private final UserService    users;

	public Backend(SessionService sessionService, UserService userService) {
		sessions = sessionService;
		users    = userService;
	}

	private void doLogin(HttpExchange ex) throws IOException {
		var body = json(ex);

		var username = body.has(USERNAME) ? body.getString(USERNAME) : null;
		var password = body.has(PASSWORD) ? body.getString(PASSWORD) : null;

		Optional<User> user = users.load(username, password);
		if (user.isPresent()) {
			var session = sessions.createSession(user.get());
			sendUserAndCookie(ex, session);
			return;
		}
		sendEmptyResponse(HTTP_UNAUTHORIZED, ex);
	}

	@Override
	public void handle(HttpExchange ex) throws IOException {
		String path   = relativePath(ex);
		String method = ex.getRequestMethod();
		System.out.printf("%s %s…", method, path);

		var session = getSession(ex);
		if ("login".equals(path) && POST.equals(method)) {
			doLogin(ex);  // TODO: prevent brute force
			return;
		}
		if (session.isEmpty()) {
			sendEmptyResponse(HTTP_UNAUTHORIZED, ex);
			System.err.println("unauthorized");
			return;
		}
		switch (path) {
			case "user":
				sendUserAndCookie(ex, session.get());
				return;
		}
		System.err.println("not implemented");
		sendEmptyResponse(HTTP_NOT_FOUND, ex);
	}

	private Optional<Session> getSession(HttpExchange ex) {
		return SessionToken.from(ex).map(SessionToken::sessionId).flatMap(sessions::retrieve);
	}

	private void sendUserAndCookie(HttpExchange ex, Session session) throws IOException {
		var bytes   = new JSONObject(session.user().map(false)).toString().getBytes(UTF_8);
		var headers = ex.getResponseHeaders();

		headers.add(CONTENT_TYPE, JSON);
		new SessionToken(session.id()).addTo(headers);
		ex.sendResponseHeaders(200, bytes.length);
		ex.getResponseBody().write(bytes);
	}
}
