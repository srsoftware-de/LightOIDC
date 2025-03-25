/* © SRSoftware 2025 */
package de.srsoftware.oidc.datastore.file;


import de.srsoftware.oidc.api.SessionService;
import de.srsoftware.oidc.api.SessionServiceTest;
import java.io.File;
import java.io.IOException;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;

public class FileStoreSessionServiceTest extends SessionServiceTest {
	private SessionService sessionService = null;

	@BeforeEach
	public void setup() throws IOException {
		var storage = new File("/tmp/" + UUID.randomUUID());
		if (storage.exists()) storage.delete();
		sessionService = new FileStore(storage, hasher());
	}

	@Override
	protected SessionService sessionService() {
		return sessionService;
	}
}
