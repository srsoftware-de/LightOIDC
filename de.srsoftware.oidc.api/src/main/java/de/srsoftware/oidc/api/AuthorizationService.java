/* © SRSoftware 2024 */
package de.srsoftware.oidc.api;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface AuthorizationService {
	AuthorizationService addCode(Client client, User user, String code);
	AuthorizationService authorize(Client client, User user, Instant expiration);
	boolean	     isAuthorized(Client client, User user);
	List<User>	     authorizedUsers(Client client);
	List<Client>	     authorizedClients(User user);
	AuthorizationService revoke(Client client, User user);

	Optional<Authorization> forCode(String code);
}
