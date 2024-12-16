/* © SRSoftware 2024 */
package de.srsoftware.oidc.datastore.file;

import de.srsoftware.oidc.api.UserService;
import de.srsoftware.oidc.api.UserServiceTest;
import de.srsoftware.tools.PasswordHasher;
import de.srsoftware.tools.UuidHasher;
import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;

public class FileStoreUserServiceTest extends UserServiceTest {
	private PasswordHasher<String> hasher = null;
	private UserService            userService;

	@Override
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
		var storage = new File("/tmp/" + UUID.randomUUID());
		if (storage.exists()) storage.delete();
		userService = new FileStore(storage, hasher());
	}

	@Override
	protected UserService userService() {
		return userService;
	}
}
