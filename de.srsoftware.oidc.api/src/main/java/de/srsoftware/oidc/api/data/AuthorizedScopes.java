/* © SRSoftware 2024 */
package de.srsoftware.oidc.api.data;

import java.time.Instant;
import java.util.Set;

public record AuthorizedScopes(Set<String> scopes, Instant expiration) {
}
