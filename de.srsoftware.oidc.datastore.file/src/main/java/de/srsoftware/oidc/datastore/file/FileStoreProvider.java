/* © SRSoftware 2024 */
package de.srsoftware.oidc.datastore.file;

import de.srsoftware.utils.UuidHasher;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;


public class FileStoreProvider extends HashMap<File, FileStore> {
	private UuidHasher hasher;

	public FileStoreProvider(UuidHasher passwordHasher) {
		hasher = passwordHasher;
	}


	@Override
	public FileStore get(Object o) {
		if (o instanceof File storageFile) try {
				var fileStore = super.get(storageFile);
				if (fileStore == null) put(storageFile, fileStore = new FileStore(storageFile, hasher));
				return fileStore;
			} catch (IOException ioex) {
				throw new RuntimeException(ioex);
			}
		return null;
	}
}
