/* © SRSoftware 2024 */
package de.srsoftware.oidc.datastore.file;

import de.srsoftware.oidc.api.KeyStorage;
import de.srsoftware.oidc.api.KeyStoreTest;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;

public class PlaintextKeyStoreTest extends KeyStoreTest {
	private KeyStorage keyStore;

	@Override
	protected KeyStorage keyStore() {
		return keyStore;
	}

	@BeforeEach
	public void setup() throws IOException {
		var storage = new File("/tmp/" + UUID.randomUUID());
		if (storage.exists()) {
			Files.walk(storage.toPath()).map(Path::toFile).forEach(File::delete);
			storage.delete();
		}
		keyStore = new PlaintextKeyStore(storage.toPath());
	}
}
