/* © SRSoftware 2024 */
package de.srsoftware.oidc.api;

import java.io.IOException;
import org.jose4j.jwk.PublicJsonWebKey;


public interface KeyManager {
	public class KeyCreationException extends Exception {
		public KeyCreationException(Exception cause) {
			super(cause);
		}
	}
	public PublicJsonWebKey getKey() throws KeyCreationException, IOException;
}
