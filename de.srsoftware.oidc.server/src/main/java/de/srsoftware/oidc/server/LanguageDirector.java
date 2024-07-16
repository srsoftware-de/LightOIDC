/* © SRSoftware 2024 */
package de.srsoftware.oidc.server;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import de.srsoftware.oidc.api.PathHandler;
import java.io.IOException;
import java.util.Arrays;

public class LanguageDirector extends PathHandler {
	private static final String DEFAULT_LANG = "de";
	private final String        path;

	public LanguageDirector(String pathTo) {
		path = pathTo;
	}

	@Override
	public void handle(HttpExchange t) throws IOException {
		Headers headers = t.getRequestHeaders();
		String  lang	= headers.get("Accept-Language").stream().flatMap(s -> Arrays.stream(s.split(","))).findFirst().orElse(DEFAULT_LANG);

		t.getResponseHeaders().add("Location", String.join(lang, "/static/", "/de/index.html"));
		t.sendResponseHeaders(301, 0);
		t.getResponseBody().close();
	}
}
