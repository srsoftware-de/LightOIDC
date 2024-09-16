/* © SRSoftware 2024 */
package de.srsoftware.oidc.api;

import de.srsoftware.oidc.api.data.AuthResult;
import de.srsoftware.oidc.api.data.Authorization;
import java.time.Instant;
import java.util.Collection;
import java.util.Optional;

public interface AuthorizationService {
	AuthorizationService    authorize(String userId, String clientId, Collection<String> scopes, String nonce, Instant expiration);
	Optional<Authorization> consumeAuthorization(String authCode);
	AuthResult	        getAuthorization(String userId, String clientId, Collection<String> scopes);
}
