/* © SRSoftware 2024 */
package de.srsoftware.oidc.api;

import java.util.List;
import java.util.Optional;

public interface ClientService {
	Optional<Client> getClient(String clientId);
	List<Client>	 listClients();
	ClientService	 remove(Client client);
	ClientService	 save(Client client);
}
