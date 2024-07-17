/* © SRSoftware 2024 */
package de.srsoftware.oidc.api;

import static java.nio.charset.StandardCharsets.UTF_8;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.json.JSONObject;

public abstract class PathHandler implements HttpHandler {
	public static final String CONTENT_TYPE = "Content-Type";
	public static final String JSON	        = "application/json";
	public static final String POST	        = "POST";

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

	/******* begin of static methods *************/

	public static String body(HttpExchange ex) throws IOException {
		return new String(ex.getRequestBody().readAllBytes(), UTF_8);
	}

	public static Optional<String> getAuthToken(HttpExchange ex) {
		return getHeader(ex, "Authorization");
	}

	public static Optional<String> getHeader(HttpExchange ex, String key) {
		return Optional.ofNullable(ex.getRequestHeaders().get(key)).map(List::stream).map(Stream::findFirst).orElse(Optional.empty());
	}

	public static JSONObject json(HttpExchange ex) throws IOException {
		return new JSONObject(body(ex));
	}

	public static Optional<String> language(HttpExchange ex) {
		return getHeader(ex, "Accept-Language").map(s -> Arrays.stream(s.split(","))).map(Stream::findFirst).orElse(Optional.empty());
	}

	public static void sendEmptyResponse(int statusCode, HttpExchange ex) throws IOException {
		ex.sendResponseHeaders(statusCode, 0);
		ex.getResponseBody().close();
	}
}
