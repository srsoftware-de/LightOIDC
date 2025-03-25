/* © SRSoftware 2025 */
package de.srsoftware.oidc.datastore.encrypted; /* © SRSoftware 2024 */
import static de.srsoftware.tools.Strings.uuid;

import de.srsoftware.oidc.api.KeyStorage;
import de.srsoftware.oidc.api.KeyStoreTest;
import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;


public class EncryptedKeyStoreTest extends KeyStoreTest {
	private class InMemoryKeyStore implements KeyStorage {
		private HashMap<String, String> store = new HashMap<>();
		@Override
		public KeyStorage drop(String keyId) {
			store.remove(keyId);
			return this;
		}

		@Override
		public List<String> listKeys() {
			return List.copyOf(store.keySet());
		}

		@Override
		public String loadJson(String keyId) {
			return store.get(keyId);
		}

		@Override
		public KeyStorage store(String keyId, String jsonWebKey) throws IOException {
			store.put(keyId, jsonWebKey);
			return this;
		}
	}
	private KeyStorage keyStore;

	@Override
	protected KeyStorage keyStore() {
		return keyStore;
	}

	@BeforeEach
	public void setup() throws SQLException {
		var backend = new InMemoryKeyStore();
		var key     = uuid();
		var salt    = uuid();
		keyStore    = new EncryptedKeyStore(key, salt, backend);
	}
}
