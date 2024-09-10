/* © SRSoftware 2024 */
package de.srsoftware.oidc.datastore.file;

import de.srsoftware.oidc.api.SessionService;
import de.srsoftware.utils.PasswordHasher;
import de.srsoftware.utils.UuidHasher;
import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;

public class SessionServiceTest {
	private PasswordHasher<String> hasher  = null;
	private File	               storage = new File("/tmp/" + UUID.randomUUID());
	private SessionService         sessionService;

	protected PasswordHasher<String> hasher() {
		if (hasher == null) try {
				hasher = new UuidHasher();
			} catch (NoSuchAlgorithmException e) {
				throw new RuntimeException(e);
			}

		return hasher;
	}

	@BeforeEach
	public void setup() throws IOException {
		if (storage.exists()) storage.delete();
		sessionService = new FileStore(storage, hasher());
	}
}
