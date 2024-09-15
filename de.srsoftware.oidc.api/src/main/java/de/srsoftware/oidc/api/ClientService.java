/* © SRSoftware 2024 */
package de.srsoftware.oidc.api;

import de.srsoftware.oidc.api.data.Client;
import java.util.List;
import java.util.Optional;

public interface ClientService {
	Optional<Client> getClient(String clientId);
	List<Client>	 listClients();
	ClientService	 remove(String clientId);
	ClientService	 save(Client client);
}
