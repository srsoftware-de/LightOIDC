/* © SRSoftware 2024 */
package de.srsoftware.oidc.api;

import java.io.IOException;
import java.util.List;
import org.jose4j.jwk.PublicJsonWebKey;

public interface KeyStorage {
	public KeyStorage       drop(String keyId);
	public List<String>     listKeys();
	public PublicJsonWebKey load(String keyId) throws IOException, KeyManager.KeyCreationException;
	public KeyStorage       store(PublicJsonWebKey jsonWebKey) throws IOException;
}
