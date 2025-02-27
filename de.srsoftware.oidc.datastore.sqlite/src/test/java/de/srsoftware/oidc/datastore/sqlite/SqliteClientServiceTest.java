/* © SRSoftware 2024 */
package de.srsoftware.oidc.datastore.sqlite;

import static de.srsoftware.tools.Strings.uuid;

import de.srsoftware.oidc.api.ClientService;
import de.srsoftware.oidc.api.ClientServiceTest;
import java.io.File;
import java.sql.SQLException;
import org.junit.jupiter.api.BeforeEach;

public class SqliteClientServiceTest extends ClientServiceTest {
	private ClientService clientService;

	@Override
	protected ClientService clientService() {
		return clientService;
	}

	@BeforeEach
	public void setup() throws SQLException {
		var dbFile    = new File("/tmp/" + uuid() + ".sqlite");
		var conn      = new ConnectionProvider().get(dbFile);
		clientService = new SqliteClientService(conn);
	}
}
