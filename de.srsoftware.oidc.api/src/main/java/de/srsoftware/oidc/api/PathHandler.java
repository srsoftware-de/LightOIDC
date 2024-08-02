/* © SRSoftware 2024 */
package de.srsoftware.oidc.api;

import static de.srsoftware.oidc.api.Constants.AUTHORIZATION;
import static java.lang.System.Logger.Level.*;
import static java.net.HttpURLConnection.*;
import static java.nio.charset.StandardCharsets.UTF_8;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import org.json.JSONObject;

public abstract class PathHandler implements HttpHandler {
	public static final String  CONTENT_TYPE   = "Content-Type";
	public static final String  DELETE         = "DELETE";
	private static final String FORWARDED_HOST = "x-forwarded-host";
	public static final String  GET	           = "GET";
	public static final String  HOST           = "host";
	public static final String  JSON           = "application/json";
	public static System.Logger LOG	           = System.getLogger(PathHandler.class.getSimpleName());
	public static final String  POST           = "POST";

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

	public boolean doDelete(String path, HttpExchange ex) throws IOException {
		return notFound(ex);
	}

	public boolean doGet(String path, HttpExchange ex) throws IOException {
		return notFound(ex);
	}

	public boolean doPost(String path, HttpExchange ex) throws IOException {
		return notFound(ex);
	}

	@Override
	public void handle(HttpExchange ex) throws IOException {
		String path   = relativePath(ex);
		String method = ex.getRequestMethod();
		LOG.log(INFO, "{0} {1}", method, path);
		boolean ignored = switch (method) {
			case DELETE -> doDelete(path,ex);
			case GET -> doGet(path,ex);
			case POST -> doPost(path,ex);
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
			return getHeader(ex, AUTHORIZATION);
		}

		public static Optional<String> getBearer(HttpExchange ex) {
			return getAuthToken(ex).filter(token -> token.startsWith("Bearer ")).map(token -> token.substring(7));
		}

		public static Optional<String> getHeader(HttpExchange ex, String key) {
			return Optional.ofNullable(ex.getRequestHeaders().get(key)).map(List::stream).flatMap(Stream::findFirst);
		}

		public static String hostname(HttpExchange ex) {
			var headers = ex.getRequestHeaders();
			var host    = headers.getFirst(FORWARDED_HOST);
			if (host == null) host = headers.getFirst(HOST);
			return host == null ? null : "https://" + host;
		}

		public static JSONObject json(HttpExchange ex) throws IOException {
			return new JSONObject(body(ex));
		}

		public static Optional<String> language(HttpExchange ex) {
			return getHeader(ex, "Accept-Language").map(s -> Arrays.stream(s.split(","))).flatMap(Stream::findFirst);
		}

		public static boolean notFound(HttpExchange ex) throws IOException {
			LOG.log(ERROR, "not implemented");
			return sendEmptyResponse(HTTP_NOT_FOUND, ex);
		}

		public static boolean sendEmptyResponse(int statusCode, HttpExchange ex) throws IOException {
			ex.sendResponseHeaders(statusCode, 0);
			return false;
		}

		public static boolean sendRedirect(HttpExchange ex, String url) throws IOException {
			ex.getResponseHeaders().add("Location", url);
			return sendEmptyResponse(HTTP_MOVED_TEMP, ex);
		}

		public static boolean sendContent(HttpExchange ex, int status, byte[] bytes) throws IOException {
			LOG.log(DEBUG, "sending {0} response…", status);
			ex.sendResponseHeaders(status, bytes.length);
			ex.getResponseBody().write(bytes);
			return true;
		}

		public static boolean sendContent(HttpExchange ex, int status, Object o) throws IOException {
			if (o instanceof Map map) o = new JSONObject(map);
			if (o instanceof JSONObject) ex.getResponseHeaders().add(CONTENT_TYPE, JSON);
			return sendContent(ex, status, o.toString().getBytes(UTF_8));
		}


		public static boolean sendContent(HttpExchange ex, byte[] bytes) throws IOException {
			return sendContent(ex, HTTP_OK, bytes);
		}

		public static boolean sendContent(HttpExchange ex, Object o) throws IOException {
			return sendContent(ex, HTTP_OK, o);
		}


		public static boolean badRequest(HttpExchange ex, byte[] bytes) throws IOException {
			return sendContent(ex, HTTP_BAD_REQUEST, bytes);
		}

		public static boolean badRequest(HttpExchange ex, Object o) throws IOException {
			return sendContent(ex, HTTP_BAD_REQUEST, o);
		}
	}
