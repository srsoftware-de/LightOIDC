/* © SRSoftware 2024 */
package de.srsoftware.oidc.datastore.file;

import static java.lang.System.Logger.Level.ERROR;

import de.srsoftware.oidc.api.KeyManager;
import de.srsoftware.oidc.api.KeyStorage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.jose4j.jwk.PublicJsonWebKey;
import org.jose4j.lang.JoseException;

public class PlaintextKeyStore implements KeyStorage {
	public static System.Logger LOG = System.getLogger(PlaintextKeyStore.class.getSimpleName());

	private final Path dir;

	public PlaintextKeyStore(Path storageDir) {
		this.dir = storageDir;
		storageDir.toFile().mkdirs();
	}
	@Override
	public KeyStorage drop(String keyId) {
		return null;
	}

	@Override
	public List<String> listKeys() {
		try {
			return Files.list(dir).map(Path::toString).filter(filename -> filename.endsWith(".key")).map(filename -> filename.substring(0, filename.length() - 4)).toList();
		} catch (IOException e) {
			LOG.log(ERROR, "Failed to list files in {0}:", dir, e);
			return List.of();
		}
	}

	@Override
	public PublicJsonWebKey load(String keyId) throws IOException, KeyManager.KeyCreationException {
		var json = Files.readString(filename(keyId));
		try {
			return PublicJsonWebKey.Factory.newPublicJwk(json);
		} catch (JoseException e) {
			throw new KeyManager.KeyCreationException(e);
		}
	}

	@Override
	public KeyStorage store(PublicJsonWebKey jsonWebKey) throws IOException {
		Files.writeString(filename(jsonWebKey.getKeyId()), jsonWebKey.toJson());
		return this;
	}

	private Path filename(String keyId) {
		return dir.resolve("%s.key".formatted(keyId));
	}
}
