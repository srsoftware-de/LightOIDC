/* © SRSoftware 2025 */
package de.srsoftware.oidc.api.data;

import java.time.Instant;

public record Session(String userId, Instant expiration, String id, boolean trustBrowser) {
}
