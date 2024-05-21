/* © SRSoftware 2024 */
package de.srsoftware.oidc.light;

import static de.srsoftware.oidc.light.Constants.MESSAGES;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Templates {
	private static Templates singleton = null;
	private static Logger LOG = LoggerFactory.getLogger(Templates.class);
	private Path dir = searchTemplates();
	private Map<String, String> messages = null;

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

	public Optional<String> get(String path) {
		return get(path, Map.of());
	}

	public Optional<String> get(String path, Map<String, String> replacements) {
		var file = dir.resolve(path);
		try {
			return Optional.of(Files.readString(file)).map(s -> replaceKeys(s, replacements));
		} catch (IOException e) {
			LOG.warn("Failed to read {}", path, e);
			return Optional.empty();
		}

	}

	private String replaceKeys(String text, Map<String, String> replacements) {
		for (Map.Entry<String, String> replacement : replacements.entrySet())
			text = text.replace(braced(replacement.getKey()), replacement.getValue());
		return text;
	}

	public Optional<String> message(String code) {
		return message(code, Map.of());
	}

	public Optional<String> message(String code, Map<String, String> replacements) {
		if (this.messages == null) {
			get(MESSAGES).map(s -> s.split("\n")).ifPresent(this::setMessages);
		}
		return Optional.ofNullable(messages.get(code)).map(text -> replaceKeys(text, replacements));
	}

	private void setMessages(String[] lines) {
		this.messages = new HashMap<>();
		for (String line : lines) {
			var parts = line.split(" ", 2);
			if (parts.length < 2) {
				LOG.warn("Invalid format in {} file, skipped {}", MESSAGES, line);
				continue;
			}
			messages.put(parts[0], parts[1]);
		}
	}

	public static String braced(String key) {
		return String.join(key, "{", "}");
	}
}
