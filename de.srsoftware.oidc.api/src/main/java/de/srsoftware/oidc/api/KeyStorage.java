/* © SRSoftware 2025 */
package de.srsoftware.oidc.api;

import java.io.IOException;
import java.util.List;
import org.jose4j.jwk.JsonWebKey;
import org.jose4j.jwk.PublicJsonWebKey;
import org.jose4j.lang.JoseException;

public interface KeyStorage {
	public KeyStorage	drop(String keyId);
	public List<String>	listKeys();
	public default PublicJsonWebKey load(String keyId) throws IOException, JoseException {
		return PublicJsonWebKey.Factory.newPublicJwk(loadJson(keyId));
	}
	public String	          loadJson(String keyId) throws IOException;
	public KeyStorage         store(String keyId, String json) throws IOException;
	public default KeyStorage store(PublicJsonWebKey key) throws IOException {
		return store(key.getKeyId(), key.toJson(JsonWebKey.OutputControlLevel.INCLUDE_PRIVATE));
	}
}
