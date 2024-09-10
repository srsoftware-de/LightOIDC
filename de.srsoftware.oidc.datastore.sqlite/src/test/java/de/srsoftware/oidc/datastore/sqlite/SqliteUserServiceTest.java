/* © SRSoftware 2024 */
package de.srsoftware.oidc.datastore.sqlite;

import de.srsoftware.oidc.api.UserService;
import de.srsoftware.oidc.api.UserServiceTest;
import java.io.File;
import java.sql.SQLException;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

public class SqliteUserServiceTest extends UserServiceTest {
	private File	    storage = new File("/tmp/" + UUID.randomUUID());
	private UserService userService;


	@AfterEach
	public void tearDown() {
		if (storage.exists()) storage.delete();
	}

	@BeforeEach
	public void setup() throws SQLException {
		tearDown();
		userService = new SqliteUserService(new ConnectionProvider().get(storage), hasher());
	}

	@Override
	protected UserService userService() {
		return userService;
	}
}
