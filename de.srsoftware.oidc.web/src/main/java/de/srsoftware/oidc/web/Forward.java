/* © SRSoftware 2024 */
package de.srsoftware.oidc.web;

import com.sun.net.httpserver.HttpExchange;
import de.srsoftware.oidc.api.PathHandler;
import java.io.IOException;

public class Forward extends PathHandler {
	private final int    CODE = 302;
	private final String toPath;

	public Forward(String toPath) {
		this.toPath = toPath;
	}

	@Override
	public void handle(HttpExchange ex) throws IOException {
		System.out.printf("Forwarding (%d) %s to %s…\n", CODE, ex.getRequestURI(), toPath);
		ex.getResponseHeaders().add("Location", toPath);
		ex.sendResponseHeaders(CODE, 0);
		ex.getResponseBody().close();
	}
}
