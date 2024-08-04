/* © SRSoftware 2024 */
package de.srsoftware.oidc.api;

import java.util.Set;

public record AuthResult(AuthorizedScopes authorizedScopes, Set<String> unauthorizedScopes, String authCode) {
}