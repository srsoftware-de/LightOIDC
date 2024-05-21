/* © SRSoftware 2024 */
package de.srsoftware;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Templates {
	private static Templates singleton = null;
	private static Logger LOG = LoggerFactory.getLogger(Templates.class);
	private Path dir = searchTemplates();

	public Templates() throws FileNotFoundException {}

	private static Path searchTemplates() throws FileNotFoundException {
		return searchTemplates(new File(System.getProperty("user.dir"))).map(File::toPath).orElseThrow(() -> new FileNotFoundException("Missing template directory"));
	}

	private static Optional<File> searchTemplates(File dir) {
		if (dir.isDirectory()) {
			var children = dir.listFiles();
			for (File child : children) {
				if (child.isDirectory()) {
					if (child.getName().equals("templates")) return Optional.of(child);
					var inner = searchTemplates(child);
					if (inner.isPresent()) return inner;
				}
			}
		}
		return Optional.empty();
	}

	public static Templates singleton() throws IOException {
		if (singleton == null) singleton = new Templates();
		return singleton;
	}

	public Optional<String> get(String path, Map<String, String> replacements) {
		var file = dir.resolve(path);
		try {
			return Optional.of(Files.readString(file)).map(s -> replaceKeys(s,replacements));
			// TODO: replacements
		} catch (IOException e) {
			LOG.warn("Failed to read {}", path, e);
			return Optional.empty();
		}

	}

	private String replaceKeys(String text, Map<String, String> replacements) {
		for (Map.Entry<String, String> replacement : replacements.entrySet()) text = text.replace("{"+replacement.getKey()+"}",replacement.getValue());
		return text;
	}
}
