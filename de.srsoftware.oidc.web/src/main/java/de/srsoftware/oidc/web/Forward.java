/* © SRSoftware 2025 */
package de.srsoftware.oidc.web;

import static java.lang.System.Logger.Level.INFO;

import com.sun.net.httpserver.HttpExchange;
import de.srsoftware.tools.Path;
import de.srsoftware.tools.PathHandler;
import java.io.IOException;

public class Forward extends PathHandler {
	private final int    CODE = 302;
	private final String toPath;

	public Forward(String toPath) {
		this.toPath = toPath;
	}

	@Override
	public boolean doGet(Path path, HttpExchange ex) throws IOException {
		LOG.log(INFO, "Forwarding ({0}}) {1} to {2}…", CODE, path, toPath);
		return sendRedirect(ex, toPath);
	}
}
