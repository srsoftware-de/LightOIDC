/* © SRSoftware 2024 */
package de.srsoftware.oidc.app;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.json.JSONObject;

public class Configuration {
	private final JSONObject json;
	private final Path       storageFile;

	public Configuration(File storage) throws IOException {
		storageFile = storage.toPath();
		if (!storage.exists()) {
			var parent = storage.getParentFile();
			if (!parent.exists() && !parent.mkdirs()) throw new FileNotFoundException("Failed to create directory %s".formatted(parent));
			Files.writeString(storageFile, "{}");
		}
		json = new JSONObject(Files.readString(storageFile));
	}

	public String getOrDefault(String key, Object defaultValue) {
		if (!json.has(key)) {
			json.put(key, defaultValue.toString());
			save();
		}
		return json.getString(key);
	}

	public Configuration save() {
		try {
			Files.writeString(storageFile, json.toString(2));
			return this;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}