package de.srsoftware.oidc.api;

import java.util.List;
import java.util.Optional;

public interface ClientService {
	Optional<Client> getClient(String clientId);
	ClientService add(Client client);
	List<Client> listClients();
	ClientService remove(Client client);
	ClientService update(Client client);
}