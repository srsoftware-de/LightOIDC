/* © SRSoftware 2024 */
package de.srsoftware.oidc.backend;

import com.sun.net.httpserver.HttpExchange;
import de.srsoftware.oidc.api.KeyStorage;
import de.srsoftware.oidc.api.PathHandler;
import java.io.IOException;

public class KeyStoreController extends PathHandler {
	private final KeyStorage keyStore;

	public KeyStoreController(KeyStorage keyStorage) {
		keyStore = keyStorage;
	}

	@Override
	public boolean doGet(String path, HttpExchange ex) throws IOException {
		return super.doGet(path, ex);
	}
}
