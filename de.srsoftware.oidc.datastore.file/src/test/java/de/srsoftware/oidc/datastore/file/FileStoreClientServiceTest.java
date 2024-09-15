/* © SRSoftware 2024 */
package de.srsoftware.oidc.datastore.file;


import de.srsoftware.oidc.api.ClientService;
import de.srsoftware.oidc.api.ClientServiceTest;
import java.io.File;
import java.io.IOException;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;

public class FileStoreClientServiceTest extends ClientServiceTest {
	private static ClientService clientService;

	@BeforeEach
	public void setup() throws IOException {
		var storage = new File("/tmp/" + UUID.randomUUID());
		if (storage.exists()) storage.delete();
		clientService = new FileStore(storage, null);
	}

	@Override
	protected ClientService clientService() {
		return clientService;
	}
}
