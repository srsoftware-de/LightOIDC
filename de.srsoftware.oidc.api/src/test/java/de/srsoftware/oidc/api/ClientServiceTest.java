/* © SRSoftware 2024 */
package de.srsoftware.oidc.api;

import static de.srsoftware.tools.Strings.uuid;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.srsoftware.oidc.api.data.Client;
import java.time.Duration;
import java.util.Set;
import org.junit.jupiter.api.Test;

public abstract class ClientServiceTest {
	private static final String NAME = "client-1";
	private static final String URI	 = "uri-1";
	private static final String URI2 = "uri-2";

	protected abstract ClientService clientService();

	@Test
	public void testSaveAndList() {
		var cs = clientService();
		assertTrue(cs.listClients().isEmpty());
		var clientId	 = uuid();
		var clientSecret = uuid();
		var landingPage	 = uuid();
		var client	 = new Client(clientId, NAME, clientSecret, Set.of(URI));
		var list	 = cs.save(client).listClients();
		assertEquals(1, list.size());
		assertTrue(list.contains(client));
		cs.remove(clientId);
		assertTrue(cs.listClients().isEmpty());
	}

	@Test
	public void testGet() {
		var cs	 = clientService();
		var clientId	 = uuid();
		var clientSecret = uuid();
		var landingPage	 = uuid();
		var client	 = new Client(clientId, NAME, clientSecret, Set.of(URI)).landingPage(landingPage).tokenValidity(Duration.ofMinutes(23));
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
		var landingPage1  = uuid();

		var client1 = new Client(clientId, NAME, clientSecret, Set.of(URI)).landingPage(landingPage1);
		var client2 = new Client(clientId, "test", clientSecret2, Set.of(URI2));

		var optClient = cs.save(client1).getClient(clientId);
		assertTrue(optClient.isPresent());
		assertEquals(client1, optClient.get());

		optClient = cs.save(client2).getClient(clientId);
		assertTrue(optClient.isPresent());
		assertEquals(client2, optClient.get());
	}
}
