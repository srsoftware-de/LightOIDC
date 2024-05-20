/* © SRSoftware 2024 */
package de.srsoftware;

import java.io.File;
import java.io.IOException;
import java.util.Optional;
import org.stringtemplate.v4.*;

public class Templates extends STRawGroupDir {
	private static Templates singleton = null;

	Templates() {
		super(templateDir(), '«', '»');
	}

	private static String templateDir() {
		return templateDir(new File(System.getProperty("user.dir"))).get().getAbsolutePath();
	}

	private static Optional<File> templateDir(File dir) {
		if (dir.isDirectory()) {
			var children = dir.listFiles();
			for (File child : children) {
				if (child.isDirectory()) {
					if (child.getName().equals("templates")) return Optional.of(child);
					var inner = templateDir(child);
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
}
