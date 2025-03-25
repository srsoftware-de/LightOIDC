/* © SRSoftware 2025 */
package de.srsoftware.oidc.api;

import de.srsoftware.oidc.api.data.AuthResult;
import de.srsoftware.oidc.api.data.Authorization;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface AuthorizationService {
	AuthorizationService    authorize(String userId, String clientId, Collection<String> scopes, Instant expiration);
	Optional<Authorization> consumeAuthorization(String authCode);
	AuthResult	        getAuthorization(String userId, String clientId, Collection<String> scopes);
	List<String>	        authorizedClients(String userId);
	Optional<String>        consumeNonce(String uuid, String id);

	void nonce(String uuid, String id, String string);
}
