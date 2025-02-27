/* © SRSoftware 2024 */
package de.srsoftware.oidc.datastore.encrypted; /* © SRSoftware 2024 */
import static de.srsoftware.tools.Optionals.nullable;
import static de.srsoftware.tools.Strings.uuid;

import de.srsoftware.oidc.api.ClientService;
import de.srsoftware.oidc.api.ClientServiceTest;
import de.srsoftware.oidc.api.data.Client;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;

public class EncryptedClientServiceTest extends ClientServiceTest {
	private static class InMemoryClientService implements ClientService {
		private HashMap<String, Client> clients = new HashMap<>();

		@Override
		public Optional<Client> getClient(String clientId) {
			return nullable(clients.get(clientId));
		}

		@Override
		public List<Client> listClients() {
			return List.copyOf(clients.values());
		}

		@Override
		public ClientService remove(String clientId) {
			clients.remove(clientId);
			return this;
		}

		@Override
		public ClientService save(Client client) {
			clients.put(client.id(), client);
			return this;
		}
	}
	private ClientService clientService;

	@Override
	protected ClientService clientService() {
		return clientService;
	}

	@BeforeEach
	public void setup() throws SQLException {
		var secret    = uuid();
		var salt      = uuid();
		var backend   = new InMemoryClientService();
		clientService = new EncryptedClientService(secret, salt, backend);
	}
}
