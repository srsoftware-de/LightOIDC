/* © SRSoftware 2024 */
package de.srsoftware.oidc.api;

import java.time.Instant;

public record Authorization(String clientId, String userId, Instant expiration) {
}
