/* © SRSoftware 2024 */
package de.srsoftware.oidc.datastore.sqlite;

import de.srsoftware.oidc.api.ClientService;
import de.srsoftware.oidc.api.data.Client;
import java.sql.Connection;
import java.util.List;
import java.util.Optional;

public class SqliteClientService implements ClientService {
	public SqliteClientService(Connection connection) {
	}

	@Override
	public Optional<Client> getClient(String clientId) {
		return Optional.empty();
	}

	@Override
	public List<Client> listClients() {
		return List.of();
	}

	@Override
	public ClientService remove(Client client) {
		return null;
	}

	@Override
	public ClientService save(Client client) {
		return null;
	}
}
