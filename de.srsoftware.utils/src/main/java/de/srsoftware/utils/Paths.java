/* © SRSoftware 2024 */
package de.srsoftware.utils;

import java.io.File;
import java.nio.file.Path;

public class Paths {
	public static Path configDir(String applicationName) {
		String home = System.getProperty("user.home");
		return Path.of(home, ".config", applicationName);
	}

	public static Path configDir(Class clazz) {
		return configDir(clazz.getSimpleName());
	}

	public static Path configDir(Object clazz) {
		return configDir(clazz.getClass());
	}

	public static String extension(File file) {
		var parts = file.getName().split("\\.");
		return parts.length == 1 ? "" : parts[parts.length - 1];
	}
}
