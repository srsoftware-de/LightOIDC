/* © SRSoftware 2024 */
package de.srsoftware.oidc.backend;


import com.sun.net.httpserver.HttpExchange;
import de.srsoftware.http.PathHandler;
import java.io.IOException;
import java.util.Map;

public class WellKnownController extends PathHandler {
	@Override
	public boolean doGet(String path, HttpExchange ex) throws IOException {
		switch (path) {
			case "/openid-configuration":
				return openidConfig(ex);
		}
		return notFound(ex);
	}

	private boolean openidConfig(HttpExchange ex) throws IOException {
		var host = hostname(ex);
		return sendContent(ex, Map.of("token_endpoint", host + "/api/token", "authorization_endpoint", host + "/web/authorization.html", "userinfo_endpoint", host + "/api/user/info", "jwks_uri", host + "/api/jwks.json", "issuer", "https://lightoidc.srsoftware.de"));
	}
}
