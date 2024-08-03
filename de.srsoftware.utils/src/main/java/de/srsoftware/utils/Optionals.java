/* © SRSoftware 2024 */
package de.srsoftware.utils;
import java.util.Optional;

import static java.util.Optional.empty;

public class Optionals {
	public static <T> Optional<T> nullable(T val) {
		return Optional.ofNullable(val);
	}

	public static Optional<String> emptyIfBlank(String text) {
		return text == null || text.isBlank() ? empty() : nullable(text.trim());
	}
}
