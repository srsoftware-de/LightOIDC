/* © SRSoftware 2024 */
package de.srsoftware.oidc.api;

import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;
import static java.net.HttpURLConnection.HTTP_OK;
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
	public static final String GET	        = "GET";
	public static final String JSON	        = "application/json";
	public static final String POST	        = "POST";

	private String[] paths;

	public class Bond {
		Bond(String[] paths) {
			PathHandler.this.paths = paths;
		}
		public HttpServer on(HttpServer server) {
			for (var path : paths) server.createContext(path, PathHandler.this);
			return server;
		}
	}

	public Bond bindPath(String... path) {
		return new Bond(path);
	}

	public boolean doGet(String path, HttpExchange ex) throws IOException {
		return false;
	}
	public boolean doPost(String path, HttpExchange ex) throws IOException {
		return false;
	}

	@Override
	public void handle(HttpExchange ex) throws IOException {
		String path   = relativePath(ex);
		String method = ex.getRequestMethod();
		System.out.printf("%s %s\n", method, path);
		boolean dummy = switch (method) {
			case POST -> doPost(path,ex);
			case GET -> doGet(path,ex);
			default -> false;
		};
		ex.getResponseBody().close();
	}

	public String relativePath(HttpExchange ex) {
		var requestPath = ex.getRequestURI().toString();
		for (var path : paths){
					if (requestPath.startsWith(path)) {
						requestPath = requestPath.substring(path.length());
						break;
					}
				}
				if (!requestPath.startsWith("/")) requestPath = "/" + requestPath;
				var pos = requestPath.indexOf("?");
				if (pos >= 0) requestPath = requestPath.substring(0, pos);
				return requestPath;
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

		public static boolean isPost(String method) {
			return POST.equals(method);
		}

		public static JSONObject json(HttpExchange ex) throws IOException {
			return new JSONObject(body(ex));
		}

		public static Optional<String> language(HttpExchange ex) {
			return getHeader(ex, "Accept-Language").map(s -> Arrays.stream(s.split(","))).map(Stream::findFirst).orElse(Optional.empty());
		}


		public static String prefix(HttpExchange ex) {
			return "http://%s".formatted(ex.getRequestHeaders().getFirst("Host"));
		}

		public static boolean sendEmptyResponse(int statusCode, HttpExchange ex) throws IOException {
			ex.sendResponseHeaders(statusCode, 0);
			return false;
		}

		public static boolean sendContent(HttpExchange ex, int status, byte[] bytes) throws IOException {
			ex.sendResponseHeaders(status, bytes.length);
			ex.getResponseBody().write(bytes);
			return true;
		}

		public static boolean sendContent(HttpExchange ex, byte[] bytes) throws IOException {
			return sendContent(ex, HTTP_OK, bytes);
		}

		public static boolean sendContent(HttpExchange ex, Object o) throws IOException {
			if (o instanceof JSONObject) ex.getResponseHeaders().add(CONTENT_TYPE, JSON);
			return sendContent(ex, HTTP_OK, o.toString().getBytes(UTF_8));
		}

		public static boolean sendError(HttpExchange ex, byte[] bytes) throws IOException {
			return sendContent(ex, HTTP_BAD_REQUEST, bytes);
		}

		public static boolean sendError(HttpExchange ex, Object o) throws IOException {
			return sendContent(ex, HTTP_BAD_REQUEST, o.toString().getBytes(UTF_8));
		}
	}
