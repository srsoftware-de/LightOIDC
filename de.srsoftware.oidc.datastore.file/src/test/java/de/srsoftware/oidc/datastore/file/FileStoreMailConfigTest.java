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
	private File	   storage;


	@Override
	protected MailConfig mailConfig() {
		return mailConfig;
	}

	@Override
	protected void reOpen() {
		try {
			mailConfig = new FileStore(storage, null);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@BeforeEach
	public void setup() throws IOException {
		storage = new File("/tmp/" + UUID.randomUUID());
		if (storage.exists()) storage.delete();
		mailConfig = new FileStore(storage, null);
	}
}
