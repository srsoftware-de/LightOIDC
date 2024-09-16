/* © SRSoftware 2024 */
package de.srsoftware.oidc.datastore.file;

import static de.srsoftware.oidc.api.Constants.*;
import static org.junit.jupiter.api.Assertions.*;

import de.srsoftware.oidc.api.MailConfig;
import de.srsoftware.oidc.api.MailConfigTest;
import java.io.File;
import java.io.IOException;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;

public class FileStoreMailConfigTest extends MailConfigTest {
	private MailConfig mailConfig;


	@Override
	protected MailConfig mailConfig() {
		return mailConfig;
	}

	@BeforeEach
	public void setup() throws IOException {
		var storage = new File("/tmp/" + UUID.randomUUID());
		if (storage.exists()) storage.delete();
		mailConfig = new FileStore(storage, null);
	}
}
