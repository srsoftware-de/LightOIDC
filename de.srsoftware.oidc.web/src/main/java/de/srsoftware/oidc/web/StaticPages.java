/* © SRSoftware 2025 */
package de.srsoftware.oidc.web;

import static java.lang.System.Logger.Level.*;
import static java.util.Optional.empty;

import com.sun.net.httpserver.HttpExchange;
import de.srsoftware.oidc.api.ResourceLoader;
import de.srsoftware.tools.PathHandler;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

public class StaticPages extends PathHandler implements ResourceLoader {
	private static final String FAVICON = "favicon.ico";
	private final Optional<Path> base;
	private ClassLoader          loader;

	public StaticPages(Optional<Path> basePath) {
		super();
		base = basePath;
	}


	private static final String INDEX = "index.html";

	@Override
	public boolean doGet(de.srsoftware.tools.Path relativePath, HttpExchange ex) throws IOException {
		var relPath = relativePath.toString();
		String lang = language(ex);
		if (relPath.isBlank()) relPath = ex.getRequestURI().toString().endsWith(FAVICON) ? FAVICON : INDEX;
		try {
			Resource resource = loadFile(lang, relPath).orElseThrow(() -> new FileNotFoundException());
			ex.getResponseHeaders().add(CONTENT_TYPE, resource.contentType());
			LOG.log(DEBUG, "Loaded {0} for language {1}…success.", relPath, lang);
			return sendContent(ex, resource.content());
		} catch (FileNotFoundException fnf) {
			LOG.log(WARNING, "Loaded {0} for language {1}…failed.", relPath, lang);
			return notFound(ex);
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

	public Optional<Resource> loadFile(String language, String path) {
		try {
			var resource = base.map(b -> getLocalUrl(b, language, path)).orElseGet(() -> getResource(language, path));
			if (resource == null) return empty();
			var connection	= resource.openConnection();
			var contentType = connection.getContentType();
			try (var in = connection.getInputStream()) {
				return Optional.of(new Resource(contentType, in.readAllBytes()));
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
