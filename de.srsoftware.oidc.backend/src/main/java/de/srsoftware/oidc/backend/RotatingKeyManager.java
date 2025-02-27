/* © SRSoftware 2024 */
package de.srsoftware.oidc.backend;

import static de.srsoftware.oidc.api.Constants.EXPIRATION;
import static de.srsoftware.tools.Optionals.nullable;
import static de.srsoftware.tools.Strings.uuid;
import static org.jose4j.jws.AlgorithmIdentifiers.RSA_USING_SHA256;

import de.srsoftware.oidc.api.KeyManager;
import de.srsoftware.oidc.api.KeyStorage;
import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
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
		for (var keyId : store.listKeys()) {
			try {
				var key     = store.load(keyId);
				var expired = nullable(key.getOtherParameterValue(EXPIRATION, Long.class)).map(Instant::ofEpochSecond).map(expiration -> expiration.isBefore(Instant.now())).orElse(false);
				if (expired) {
					store.drop(keyId);
				} else
					return key;
			} catch (Exception e) {
				LOG.log(System.Logger.Level.WARNING, "Failed to load key with id {0}", keyId);
			}
		}
		return createNewKey();
	}

	private PublicJsonWebKey createNewKey() throws KeyCreationException, IOException {
		try {
			var key = RsaJwkGenerator.generateJwk(2048);
			key.setAlgorithm(RSA_USING_SHA256);
			key.setKeyId(uuid());
			key.setOtherParameter(EXPIRATION, Instant.now().plus(1, ChronoUnit.DAYS).getEpochSecond());
			store.store(key);
			LOG.log(System.Logger.Level.INFO, "Created new JsonWebKey (Id: {0})", key.getKeyId());
			return key;
		} catch (JoseException e) {
			throw new KeyCreationException(e);
		}
	}
}
