/* © SRSoftware 2024 */
package de.srsoftware.oidc.api;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface ClaimAuthorizationService {
	public record AuthResult(List<AuthorizedScope> authorizedScopes, Set<String> unauthorizedScopes, String authCode) {
	}
	AuthResult	          getAuthorization(User user, Client client, Collection<String> scopes);
	ClaimAuthorizationService authorize(User user, Client client, Collection<String> scopes, Instant expiration);
}
