/* © SRSoftware 2024 */
package de.srsoftware.oidc.datastore.file;

import static org.junit.jupiter.api.Assertions.*;

import de.srsoftware.oidc.api.AuthServiceTest;
import de.srsoftware.oidc.api.AuthorizationService;
import java.io.File;
import java.io.IOException;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;

public class FileStoreAuthServiceTest extends AuthServiceTest {
	private File	             storage = new File("/tmp/" + UUID.randomUUID());
	private AuthorizationService authorizationService;

	@Override
	protected AuthorizationService authorizationService() {
		return authorizationService;
	}

	@BeforeEach
	public void setup() throws IOException {
		if (storage.exists()) storage.delete();
		authorizationService = new FileStore(storage, null);
	}
}
