/* © SRSoftware 2024 */
package de.srsoftware.oidc.datastore.sqlite;


import de.srsoftware.oidc.api.SessionService;
import de.srsoftware.oidc.api.SessionServiceTest;
import java.io.File;
import java.sql.SQLException;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

public class SqliteSessionServiceTest extends SessionServiceTest {
	private File	       storage	      = new File("/tmp/" + UUID.randomUUID());
	private SessionService sessionService = null;

	@AfterEach
	public void tearDown() {
		if (storage.exists()) storage.delete();
	}

	@BeforeEach
	public void setup() throws SQLException {
		tearDown();
		sessionService = new SqliteSessionService(new ConnectionProvider().get(storage));
	}

	@Override
	protected SessionService sessionService() {
		return sessionService;
	}
}
