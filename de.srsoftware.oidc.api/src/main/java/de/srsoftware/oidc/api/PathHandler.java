/* © SRSoftware 2024 */
package de.srsoftware.oidc.api;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

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

	public Optional<String> getHeader(HttpExchange ex, String key) {
		return Optional.ofNullable(ex.getRequestHeaders().get(key)).map(List::stream).map(Stream::findFirst).orElse(Optional.empty());
	}

	public Optional<String> language(HttpExchange ex) {
		return getHeader(ex, "Accept-Language").map(s -> Arrays.stream(s.split(","))).map(Stream::findFirst).orElse(Optional.empty());
	}

	public void emptyResponse(int statusCode, HttpExchange ex) throws IOException {
		ex.sendResponseHeaders(statusCode, 0);
		ex.getResponseBody().close();
	}
}
