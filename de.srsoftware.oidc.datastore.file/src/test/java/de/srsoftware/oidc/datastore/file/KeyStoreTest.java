/* © SRSoftware 2024 */
package de.srsoftware.oidc.datastore.file;

import static de.srsoftware.oidc.api.Constants.EXPIRATION;
import static de.srsoftware.utils.Strings.uuid;
import static org.jose4j.jws.AlgorithmIdentifiers.RSA_USING_SHA256;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.srsoftware.oidc.api.KeyManager;
import de.srsoftware.oidc.api.KeyStorage;
import de.srsoftware.utils.PasswordHasher;
import de.srsoftware.utils.UuidHasher;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.UUID;
import org.jose4j.jwk.RsaJwkGenerator;
import org.jose4j.lang.JoseException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class KeyStoreTest {
	private File	   storage = new File("/tmp/" + UUID.randomUUID());
	private UuidHasher hasher;
	private KeyStorage keyStore;


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
		if (storage.exists()) {
			Files.walk(storage.toPath()).map(Path::toFile).forEach(File::delete);
			storage.delete();
		}
		keyStore = new PlaintextKeyStore(storage.toPath());
	}

	@Test
	void testStoreAndLoad() throws JoseException, IOException, KeyManager.KeyCreationException {
		var keyId = uuid();
		var key   = RsaJwkGenerator.generateJwk(2048);
		key.setAlgorithm(RSA_USING_SHA256);
		key.setKeyId(keyId);
		key.setOtherParameter(EXPIRATION, Instant.now().plus(1, ChronoUnit.DAYS).getEpochSecond());
		keyStore.store(key);
		var loaded = keyStore.load(keyId);
		assertEquals(key.toJson(), loaded.toJson());
	}

	@Test
	void testListKeys() throws JoseException, IOException {
		var keyId1 = uuid();
		var key1   = RsaJwkGenerator.generateJwk(2048);
		key1.setAlgorithm(RSA_USING_SHA256);
		key1.setKeyId(keyId1);
		key1.setOtherParameter(EXPIRATION, Instant.now().plus(1, ChronoUnit.DAYS).getEpochSecond());
		keyStore.store(key1);

		var keyId2 = uuid();
		var key2   = RsaJwkGenerator.generateJwk(2048);
		key2.setAlgorithm(RSA_USING_SHA256);
		key2.setKeyId(keyId2);
		key2.setOtherParameter(EXPIRATION, Instant.now().plus(1, ChronoUnit.DAYS).getEpochSecond());
		keyStore.store(key2);

		var keyIds = keyStore.listKeys();
		assertEquals(2, keyIds.size());
		assertTrue(keyIds.containsAll(Set.of(keyId1, keyId2)));
	}

	@Test
	void testDrop() throws IOException, JoseException {
		var keyId1 = uuid();
		var key1   = RsaJwkGenerator.generateJwk(2048);
		key1.setAlgorithm(RSA_USING_SHA256);
		key1.setKeyId(keyId1);
		key1.setOtherParameter(EXPIRATION, Instant.now().plus(1, ChronoUnit.DAYS).getEpochSecond());
		keyStore.store(key1);

		var keyId2 = uuid();
		var key2   = RsaJwkGenerator.generateJwk(2048);
		key2.setAlgorithm(RSA_USING_SHA256);
		key2.setKeyId(keyId2);
		key2.setOtherParameter(EXPIRATION, Instant.now().plus(1, ChronoUnit.DAYS).getEpochSecond());
		keyStore.store(key2);

		assertEquals(2, keyStore.listKeys().size());
		keyStore.drop("unknown");
		assertEquals(2, keyStore.listKeys().size());
		keyStore.drop(keyId2);
		assertEquals(1, keyStore.listKeys().size());
		assertTrue(keyStore.listKeys().contains(keyId1));
		keyStore.drop(keyId1);
		assertTrue(keyStore.listKeys().isEmpty());
	}
}
