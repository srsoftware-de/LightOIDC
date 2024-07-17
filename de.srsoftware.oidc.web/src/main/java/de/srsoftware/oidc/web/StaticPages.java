/* © SRSoftware 2024 */
package de.srsoftware.oidc.web;

import com.sun.net.httpserver.HttpExchange;
import de.srsoftware.oidc.api.PathHandler;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Optional;

public class StaticPages extends PathHandler {
	private static final String DEFAULT_LANGUAGE = "en";
	private ClassLoader         loader;

	private record Response(String contentType, byte[] content) {
	}
	private static final String INDEX = "en/index.html";

	@Override
	public void handle(HttpExchange ex) throws IOException {
		String path   = relativePath(ex);
		String lang   = language(ex).orElse(DEFAULT_LANGUAGE);
		String method = ex.getRequestMethod();

		if (path.isBlank()) path = INDEX;
		System.out.printf("%s %s: ", method, ex.getRequestURI());
		try {
			System.out.printf("Loading %s for lagnuage %s…", path, lang);
			var response = loadTemplate(lang, path).orElseThrow(() -> new FileNotFoundException());

			ex.getResponseHeaders().add(CONTENT_TYPE, response.contentType);
			ex.sendResponseHeaders(200, response.content.length);
			OutputStream os = ex.getResponseBody();
			os.write(response.content);
			os.close();
			System.out.println("success.");
		} catch (FileNotFoundException fnf) {
			ex.sendResponseHeaders(404, 0);
			ex.getResponseBody().close();
			System.err.println("failed!");
		}
	}

	private Optional<Response> loadTemplate(String language, String path) throws IOException {
		if (loader == null) loader = getClass().getClassLoader();
		var resource = loader.getResource(String.join("/", language, path));
		if (resource == null) resource = loader.getResource(String.join("/", DEFAULT_LANGUAGE, path));
		if (resource == null) return Optional.empty();
		var connection	= resource.openConnection();
		var contentType = connection.getContentType();
		try (var in = connection.getInputStream()) {
			return Optional.of(new Response(contentType, in.readAllBytes()));
		}
	}
}
