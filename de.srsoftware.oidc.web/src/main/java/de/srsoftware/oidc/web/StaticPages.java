/* © SRSoftware 2024 */
package de.srsoftware.oidc.web;

import com.sun.net.httpserver.HttpExchange;
import de.srsoftware.oidc.api.PathHandler;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Optional;

public class StaticPages extends PathHandler {
	private static final String DEFAULT_LANG = "en";
	private ClassLoader         loader;

	private record Response(String contentType, byte[] content) {
	}
	private static final String INDEX = "de/index.html";

	@Override
	public void handle(HttpExchange ex) throws IOException {
		String path = relativePath(ex);
		if (path.isBlank()) path = INDEX;
		try {
			var response = loadTemplate(path).orElseThrow(() -> new FileNotFoundException());

			ex.getResponseHeaders().add("Content-Type", response.contentType);
			ex.sendResponseHeaders(200, response.content.length);
			OutputStream os = ex.getResponseBody();
			os.write(response.content);
			os.close();
		} catch (FileNotFoundException fnf) {
			ex.sendResponseHeaders(404, 0);
			ex.getResponseBody().close();
		}
	}

	private Optional<Response> loadTemplate(String path) throws IOException {
		if (loader == null) loader = getClass().getClassLoader();
		var resource = loader.getResource(path);
		if (resource == null) {
			var parts = path.split("/");
			parts[0]  = DEFAULT_LANG;
			path      = String.join("/", parts);
			resource  = loader.getResource(path);
		}
		if (resource == null) return Optional.empty();
		var connection	= resource.openConnection();
		var contentType = connection.getContentType();
		try (var in = connection.getInputStream()) {
			return Optional.of(new Response(contentType, in.readAllBytes()));
		}
	}
}
