/* © SRSoftware 2024 */
package de.srsoftware.oidc.api;

import java.time.Instant;
import java.util.Collection;
import java.util.Optional;

public interface ClaimAuthorizationService {
	ClaimAuthorizationService authorize(User user, Client client, Collection<String> scopes, Instant expiration);
	Optional<Authorization>   consumeAuthorization(String authCode);
	AuthResult	          getAuthorization(User user, Client client, Collection<String> scopes);
}
