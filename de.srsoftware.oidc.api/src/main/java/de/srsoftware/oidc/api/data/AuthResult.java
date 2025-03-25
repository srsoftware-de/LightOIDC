/* © SRSoftware 2025 */
package de.srsoftware.oidc.api.data;

import java.util.Set;

public record AuthResult(AuthorizedScopes authorizedScopes, Set<String> unauthorizedScopes, String authCode) {
}