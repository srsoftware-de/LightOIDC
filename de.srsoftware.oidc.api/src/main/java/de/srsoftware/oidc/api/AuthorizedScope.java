/* © SRSoftware 2024 */
package de.srsoftware.oidc.api;

import java.time.Instant;

public record AuthorizedScope(String scope, Instant expiration) {
}
