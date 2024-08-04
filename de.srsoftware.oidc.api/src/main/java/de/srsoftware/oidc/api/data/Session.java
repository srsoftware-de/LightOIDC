/* © SRSoftware 2024 */
package de.srsoftware.oidc.api.data;

import java.time.Instant;

public record Session(User user, Instant expiration, String id) {
}
