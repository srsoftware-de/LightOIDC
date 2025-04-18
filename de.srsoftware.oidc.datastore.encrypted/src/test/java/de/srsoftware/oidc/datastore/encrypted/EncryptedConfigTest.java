/* © SRSoftware 2025 */
package de.srsoftware.oidc.datastore.encrypted; /* © SRSoftware 2024 */
import static de.srsoftware.tools.Strings.uuid;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class EncryptedConfigTest {
	@Test
	public void testEncryptionDecryption() {
		var key       = uuid();
		var salt      = uuid();
		var secret    = uuid();
		var encryptor = new EncryptedConfig(key, salt);
		var decryptor = new EncryptedConfig(key, salt);
		var encrypted = encryptor.encrypt(secret);
		var decrypted = decryptor.decrypt(encrypted);
		assertEquals(secret, decrypted);
	}
}
