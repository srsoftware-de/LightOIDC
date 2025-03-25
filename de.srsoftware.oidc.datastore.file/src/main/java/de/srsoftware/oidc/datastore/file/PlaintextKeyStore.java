/* © SRSoftware 2025 */
package de.srsoftware.oidc.datastore.file;

import static java.lang.System.Logger.Level.DEBUG;
import static java.lang.System.Logger.Level.ERROR;

import de.srsoftware.oidc.api.KeyStorage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;

public class PlaintextKeyStore implements KeyStorage {
	public static System.Logger LOG = System.getLogger(PlaintextKeyStore.class.getSimpleName());

	private final Path	dir;
	private HashMap<String, String> loaded = new HashMap<>();

	public PlaintextKeyStore(Path storageDir) {
		this.dir = storageDir;
		storageDir.toFile().mkdirs();
	}
	@Override
	public KeyStorage drop(String keyId) {
		if (dir.resolve(keyId + ".key").toFile().delete()) LOG.log(DEBUG, "Removed key {0}", keyId);
		return this;
	}

	@Override
	public List<String> listKeys() {
		try {
			return Files.list(dir).map(Path::getFileName).map(Path::toString).filter(filename -> filename.endsWith(".key")).map(filename -> filename.substring(0, filename.length() - 4)).toList();
		} catch (IOException e) {
			LOG.log(ERROR, "Failed to list files in {0}:", dir, e);
			return List.of();
		}
	}

	@Override
	public String loadJson(String keyId) throws IOException {
		var key = loaded.get(keyId);
		if (key != null) return key;
		key = Files.readString(filename(keyId));
		loaded.put(keyId, key);
		return key;
	}

	@Override
	public KeyStorage store(String keyId, String jsonWebKey) throws IOException {
		Files.writeString(filename(keyId), jsonWebKey);
		return this;
	}

	private Path filename(String keyId) {
		return dir.resolve("%s.key".formatted(keyId));
	}
}
