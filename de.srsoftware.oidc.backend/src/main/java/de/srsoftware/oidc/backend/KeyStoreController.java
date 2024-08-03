/* © SRSoftware 2024 */
package de.srsoftware.oidc.backend;

import com.sun.net.httpserver.HttpExchange;
import de.srsoftware.oidc.api.KeyManager;
import de.srsoftware.oidc.api.KeyStorage;
import de.srsoftware.oidc.api.PathHandler;
import java.io.IOException;
import org.jose4j.jwk.JsonWebKey;
import org.jose4j.jwk.PublicJsonWebKey;
import org.json.JSONArray;
import org.json.JSONObject;

public class KeyStoreController extends PathHandler {
	private final KeyStorage keyStore;

	public KeyStoreController(KeyStorage keyStorage) {
		keyStore = keyStorage;
	}

	@Override
	public boolean doGet(String path, HttpExchange ex) throws IOException {
		switch (path) {
			case "/":
				return jwksJson(ex);
		}
		return notFound(ex);
	}

	private boolean jwksJson(HttpExchange ex) throws IOException {
		JSONArray arr = new JSONArray();
		for (var keyId : keyStore.listKeys()) try {
				PublicJsonWebKey key     = keyStore.load(keyId);
				String	 keyJson = key.toJson(JsonWebKey.OutputControlLevel.PUBLIC_ONLY);
				arr.put(new JSONObject(keyJson));
			} catch (IOException e) {
				throw new RuntimeException(e);
			} catch (KeyManager.KeyCreationException e) {
				throw new RuntimeException(e);
			}
		JSONObject result = new JSONObject();
		result.put("keys", arr);
		return sendContent(ex, result);
	}
}