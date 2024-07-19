/* © SRSoftware 2024 */
package de.srsoftware.oidc.web;

import com.sun.net.httpserver.HttpExchange;
import de.srsoftware.oidc.api.PathHandler;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

public class StaticPages extends PathHandler {
	private static final String DEFAULT_LANGUAGE = "en";
	private final Optional<Path> base;
	private ClassLoader          loader;

	public StaticPages(Optional<Path> basePath) {
		super();
		base = basePath;
	}

	private record Response(String contentType, byte[] content) {
	}
	private static final String INDEX = "en/index.html";

	@Override
	public void handle(HttpExchange ex) throws IOException {
		String relativePath = relativePath(ex);
		String lang	    = language(ex).orElse(DEFAULT_LANGUAGE);
		String method	    = ex.getRequestMethod();

		if (relativePath.isBlank()) relativePath = INDEX;
		System.out.printf("%s %s: ", method, ex.getRequestURI());
		try {
			System.out.printf("Loading %s for lagnuage %s…", relativePath, lang);
			Response response = loadFile(lang, relativePath).orElseThrow(() -> new FileNotFoundException());

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

	private URL getLocalUrl(Path base, String language, String path) {
		var file = base.resolve(language).resolve(path);
		if (!Files.isRegularFile(file)) {
			file = base.resolve(DEFAULT_LANGUAGE).resolve(path);
			if (!Files.isRegularFile(file)) return null;
		}
		try {
			return file.toUri().toURL();
		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		}
	}

	private URL getResource(String language, String path) {
		if (loader == null) loader = getClass().getClassLoader();
		var resource = loader.getResource(String.join("/", language, path));
		if (resource == null) resource = loader.getResource(String.join("/", DEFAULT_LANGUAGE, path));
		return resource;
	}

	private Optional<Response> loadFile(String language, String path) {
		try {
			var resource = base.map(b -> getLocalUrl(b, language, path)).orElseGet(() -> getResource(language, path));
			if (resource == null) return Optional.empty();
			var connection	= resource.openConnection();
			var contentType = connection.getContentType();
			try (var in = connection.getInputStream()) {
				return Optional.of(new Response(contentType, in.readAllBytes()));
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
