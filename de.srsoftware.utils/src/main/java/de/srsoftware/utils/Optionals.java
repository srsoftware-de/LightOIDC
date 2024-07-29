/* © SRSoftware 2024 */
package de.srsoftware.utils;
import java.util.Optional;

public class Optionals {
	public static <T> Optional<T> optional(T val) {
		return Optional.ofNullable(val);
	}

	public static Optional<String> nonEmpty(String text) {
		return text == null || text.isBlank() ? Optional.empty() : optional(text.trim());
	}
}
