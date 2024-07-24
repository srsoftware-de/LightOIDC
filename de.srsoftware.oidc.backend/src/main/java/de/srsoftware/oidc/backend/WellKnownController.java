/* © SRSoftware 2024 */
package de.srsoftware.oidc.backend;

import static java.lang.System.Logger.Level.WARNING;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;

import com.sun.net.httpserver.HttpExchange;
import de.srsoftware.oidc.api.PathHandler;
import java.io.IOException;
import java.util.Map;

public class WellKnownController extends PathHandler {
	@Override
	public boolean doGet(String path, HttpExchange ex) throws IOException {
		switch (path) {
			case "/openid-configuration":
				return openidConfig(ex);
		}
		LOG.log(WARNING, "not implemented");
		return sendEmptyResponse(HTTP_NOT_FOUND, ex);
	}

	private boolean openidConfig(HttpExchange ex) throws IOException {
		var host = hostname(ex);
		return sendContent(ex, Map.of("token_endpoint", host + "/api/token", "authorization_endpoint", host + "/web/authorization.html", "userinfo_endpoint", host + "/api/userinfo", "jwks_uri", host + "/api/jwks"));
	}
}
