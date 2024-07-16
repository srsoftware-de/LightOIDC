/* © SRSoftware 2024 */
package de.srsoftware.oidc.api;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

public abstract class PathHandler implements HttpHandler {
	private String path;

	public class Bond {
		Bond(String p) {
			path = p;
		}
		public HttpServer on(HttpServer server) {
			server.createContext(path, PathHandler.this);
			return server;
		}
	}

	public Bond bindPath(String path) {
		return new Bond(path);
	}

	public String relativePath(HttpExchange ex) {
		var path = ex.getRequestURI().toString();
		if (path.startsWith(this.path)) path = path.substring(this.path.length());
		if (path.startsWith("/")) path = path.substring(1);
		return path;
	}
}
