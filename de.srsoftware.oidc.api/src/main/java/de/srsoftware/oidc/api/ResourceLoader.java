/* © SRSoftware 2024 */
package de.srsoftware.oidc.api;

import java.util.Optional;

public interface ResourceLoader {
	public static record Resource(String contentType, byte[] content) {
	}
	Optional<Resource> loadFile(String lang, String path);
}
