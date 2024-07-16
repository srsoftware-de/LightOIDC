/* © SRSoftware 2024 */
package de.srsoftware.oidc.backend;

import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static java.net.HttpURLConnection.HTTP_UNAUTHORIZED;

import com.sun.net.httpserver.HttpExchange;
import de.srsoftware.oidc.api.PathHandler;
import java.io.IOException;
import java.util.Optional;

public class Backend extends PathHandler {
	@Override
	public void handle(HttpExchange ex) throws IOException {
		String path   = relativePath(ex);
		String method = ex.getRequestMethod();
		System.out.printf("%s %s…", method, path);

		if ("login".equals(path)) {
			doLogin(ex); // TODO: prevent brute force
			return;
		}
		var token = getAuthToken(ex);
		if (token.isEmpty()) {
			emptyResponse(HTTP_UNAUTHORIZED, ex);
			System.err.println("unauthorized");
			return;
		}
		System.err.println("not implemented");
		ex.sendResponseHeaders(HTTP_NOT_FOUND, 0);
		ex.getResponseBody().close();
	}

	private void doLogin(HttpExchange ex) throws IOException {
		Optional<String> user = getHeader(ex, "login-username");
		Optional<String> pass = getHeader(ex, "login-password");
		System.out.printf("%s : %s", user, pass);
		emptyResponse(HTTP_UNAUTHORIZED, ex);
	}

	private Optional<String> getAuthToken(HttpExchange ex) {
		return getHeader(ex, "Authorization");
	}
}
