/* © SRSoftware 2024 */
package de.srsoftware.oidc.api;

import java.util.Date;
import java.util.List;

public interface AuthorizationService {
	AuthorizationService authorize(Client client, User user, Date expiration);
	boolean	     isAuthorized(Client client, User user);
	List<User>	     authorizedUsers(Client client);
	List<Client>	     authorizedClients(User user);
	AuthorizationService revoke(Client client, User user);
}
