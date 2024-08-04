/* © SRSoftware 2024 */
package de.srsoftware.oidc.api;

import de.srsoftware.oidc.api.data.AuthResult;
import de.srsoftware.oidc.api.data.Authorization;
import de.srsoftware.oidc.api.data.Client;
import de.srsoftware.oidc.api.data.User;
import java.time.Instant;
import java.util.Collection;
import java.util.Optional;

public interface AuthorizationService {
	AuthorizationService    authorize(User user, Client client, Collection<String> scopes, Instant expiration);
	Optional<Authorization> consumeAuthorization(String authCode);
	AuthResult	        getAuthorization(User user, Client client, Collection<String> scopes);
}
