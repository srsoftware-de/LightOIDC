/* © SRSoftware 2024 */
package de.srsoftware.oidc.datastore.file;

import static de.srsoftware.utils.Strings.uuid;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.srsoftware.oidc.api.ClientService;
import de.srsoftware.oidc.api.data.Client;
import java.io.File;
import java.io.IOException;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ClientServiceTest {
	private static ClientService clientService;
	private static final String  NAME = "client-1";
	private static final String  URI  = "uri-1";
	private static final String  URI2 = "uri-2";
	@BeforeEach
	public void setup() throws IOException {
		var storage = new File("/tmp/" + UUID.randomUUID());
		if (storage.exists()) storage.delete();
		clientService = new FileStore(storage, null);
	}

	protected ClientService clientService() {
		return clientService;
	}

	@Test
	public void testSaveAndList() {
		var cs = clientService();
		assertTrue(cs.listClients().isEmpty());
		var clientId	 = uuid();
		var clientSecret = uuid();
		var client	 = new Client(clientId, NAME, clientSecret, Set.of(URI));
		var list	 = cs.save(client).listClients();
		assertEquals(1, list.size());
		assertTrue(list.contains(client));
		cs.remove(client);
		assertTrue(cs.listClients().isEmpty());
	}

	@Test
	public void testGet() {
		var cs	 = clientService();
		var clientId	 = uuid();
		var clientSecret = uuid();
		var client	 = new Client(clientId, NAME, clientSecret, Set.of(URI));
		var optClient	 = cs.save(client).getClient(clientId);
		assertTrue(optClient.isPresent());
		assertEquals(client, optClient.get());
		optClient = cs.getClient(uuid());
		assertTrue(optClient.isEmpty());
	}

	@Test
	public void testOverride() {
		var cs	  = clientService();
		var clientId	  = uuid();
		var clientSecret  = uuid();
		var clientSecret2 = uuid();
		var client1	  = new Client(clientId, NAME, clientSecret, Set.of(URI));
		var client2	  = new Client(clientId, "test", clientSecret2, Set.of(URI2));

		var optClient = cs.save(client1).getClient(clientId);
		assertTrue(optClient.isPresent());
		assertEquals(client1, optClient.get());

		optClient = cs.save(client2).getClient(clientId);
		assertTrue(optClient.isPresent());
		assertEquals(client2, optClient.get());
	}
}
