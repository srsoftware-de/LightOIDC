/* © SRSoftware 2024 */
package de.srsoftware.oidc.api;

import java.time.Instant;

public record Session(User user, Instant expiration, String id) {
}
