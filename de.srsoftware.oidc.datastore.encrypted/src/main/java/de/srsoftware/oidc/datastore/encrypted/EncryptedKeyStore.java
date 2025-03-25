/* © SRSoftware 2025 */
package de.srsoftware.oidc.datastore.encrypted;

import de.srsoftware.oidc.api.KeyStorage;
import java.io.IOException;
import java.util.List;

public class EncryptedKeyStore extends EncryptedConfig implements KeyStorage {
	private final KeyStorage backend;

	public EncryptedKeyStore(String key, String salt, KeyStorage backend) {
		super(key, salt);
		this.backend = backend;
	}

	@Override
	public KeyStorage drop(String keyId) {
		return backend.drop(keyId);
	}

	@Override
	public List<String> listKeys() {
		return backend.listKeys();
	}

	@Override
	public String loadJson(String keyId) throws IOException {
		return decrypt(backend.loadJson(keyId));
	}

	@Override
	public KeyStorage store(String keyId, String jsonWebKey) throws IOException {
		backend.store(keyId, encrypt(jsonWebKey));
		return this;
	}
}
