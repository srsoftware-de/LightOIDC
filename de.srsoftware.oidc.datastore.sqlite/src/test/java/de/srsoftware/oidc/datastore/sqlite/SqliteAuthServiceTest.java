/* © SRSoftware 2024 */
package de.srsoftware.oidc.datastore.sqlite;

import static de.srsoftware.tools.Strings.uuid;

import de.srsoftware.oidc.api.AuthServiceTest;
import de.srsoftware.oidc.api.AuthorizationService;
import java.io.File;
import java.sql.SQLException;
import org.junit.jupiter.api.BeforeEach;

public class SqliteAuthServiceTest extends AuthServiceTest {
	private AuthorizationService authorizationService;

	@Override
	protected AuthorizationService authorizationService() {
		return authorizationService;
	}

	@BeforeEach
	public void setup() throws SQLException {
		var dbFile	     = new File("/tmp/" + uuid() + ".sqlite");
		var conn	     = new ConnectionProvider().get(dbFile);
		authorizationService = new SqliteAuthService(conn);
	}
}
