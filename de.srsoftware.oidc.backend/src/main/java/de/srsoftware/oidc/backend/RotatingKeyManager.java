/* © SRSoftware 2024 */
package de.srsoftware.oidc.backend;

import static de.srsoftware.utils.Strings.uuid;
import static org.jose4j.jws.AlgorithmIdentifiers.RSA_USING_SHA256;

import de.srsoftware.oidc.api.KeyManager;
import de.srsoftware.oidc.api.KeyStorage;
import java.io.IOException;
import org.jose4j.jwk.PublicJsonWebKey;
import org.jose4j.jwk.RsaJwkGenerator;
import org.jose4j.lang.JoseException;

public class RotatingKeyManager implements KeyManager {
	private static final System.Logger LOG = System.getLogger(RotatingKeyManager.class.getSimpleName());
	private final KeyStorage	   store;

	public RotatingKeyManager(KeyStorage keyStore) {
		store = keyStore;
	}

	@Override
	public PublicJsonWebKey getKey() throws KeyCreationException, IOException {
		var list = store.listKeys();
		return list.isEmpty() ? createNewKey() : store.load(list.get(0));
	}

	private PublicJsonWebKey createNewKey() throws KeyCreationException, IOException {
		try {
			var key = RsaJwkGenerator.generateJwk(2048);
			key.setAlgorithm(RSA_USING_SHA256);
			key.setKeyId(uuid());
			store.store(key);
			return key;
		} catch (JoseException e) {
			throw new KeyCreationException(e);
		}
	}
}
