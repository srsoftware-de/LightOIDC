/* © SRSoftware 2024 */
package de.srsoftware.oidc.backend;

import static de.srsoftware.oidc.api.User.PASSWORD;
import static de.srsoftware.oidc.api.User.USERNAME;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static java.net.HttpURLConnection.HTTP_UNAUTHORIZED;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.jar.Attributes.Name.CONTENT_TYPE;

import com.sun.net.httpserver.HttpExchange;
import de.srsoftware.oidc.api.PathHandler;
import de.srsoftware.oidc.api.SessionToken;
import de.srsoftware.oidc.api.User;
import de.srsoftware.oidc.api.UserService;
import java.io.IOException;
import java.util.Optional;
import org.json.JSONObject;

public class Backend extends PathHandler {
	private final UserService users;

	public Backend(UserService userService) {
		users = userService;
	}

	private void doLogin(HttpExchange ex) throws IOException {
		var body = json(ex);

		var username = body.has(USERNAME) ? body.getString(USERNAME) : null;
		var password = body.has(PASSWORD) ? body.getString(PASSWORD) : null;

		Optional<User> user = users.load(username, password);
		if (user.isPresent()) {
			sendUserAndCookie(ex, user.get());
			return;
		}
		sendEmptyResponse(HTTP_UNAUTHORIZED, ex);
	}

	@Override
	public void handle(HttpExchange ex) throws IOException {
		String path   = relativePath(ex);
		String method = ex.getRequestMethod();
		System.out.printf("%s %s…", method, path);

		if ("login".equals(path) && POST.equals(method)) {
			doLogin(ex);  // TODO: prevent brute force
			return;
		}
		var token = getAuthToken(ex);
		if (token.isEmpty()) {
			sendEmptyResponse(HTTP_UNAUTHORIZED, ex);
			System.err.println("unauthorized");
			return;
		}
		System.err.println("not implemented");
		ex.sendResponseHeaders(HTTP_NOT_FOUND, 0);
		ex.getResponseBody().close();
	}

	private void sendUserAndCookie(HttpExchange ex, User user) throws IOException {
		var bytes   = new JSONObject(user.map(false)).toString().getBytes(UTF_8);
		var headers = ex.getResponseHeaders();

		headers.add(CONTENT_TYPE, JSON);
		new SessionToken("Test").addTo(headers);
		ex.sendResponseHeaders(200, bytes.length);
		ex.getResponseBody().write(bytes);
	}
}
