/* © SRSoftware 2024 */
import static de.srsoftware.utils.Strings.uuid;
import static org.junit.jupiter.api.Assertions.assertEquals;

import de.srsoftware.oidc.datastore.encrypted.EncryptedConfig;
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
