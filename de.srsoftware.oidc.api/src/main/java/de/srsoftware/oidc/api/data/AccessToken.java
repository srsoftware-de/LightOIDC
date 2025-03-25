/* © SRSoftware 2025 */
package de.srsoftware.oidc.api.data;

import java.time.Instant;

public record AccessToken(String id, User user, Instant expiration) {
	public boolean valid() {
		return Instant.now().isBefore(expiration);
	}
}
