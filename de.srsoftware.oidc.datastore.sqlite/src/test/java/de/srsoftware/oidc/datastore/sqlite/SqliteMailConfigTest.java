/* © SRSoftware 2024 */
package de.srsoftware.oidc.datastore.sqlite;

import static de.srsoftware.tools.Strings.uuid;

import de.srsoftware.oidc.api.MailConfig;
import de.srsoftware.oidc.api.MailConfigTest;
import java.io.File;
import java.sql.SQLException;
import org.junit.jupiter.api.BeforeEach;

public class SqliteMailConfigTest extends MailConfigTest {
	private SqliteMailConfig mailConfig;
	private File	         dbFile;

	@Override
	protected MailConfig mailConfig() {
		return mailConfig;
	}

	@Override
	protected void reOpen() {
		try {
			var conn   = new ConnectionProvider().get(dbFile);
			mailConfig = new SqliteMailConfig(conn);
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	@BeforeEach
	public void setup() throws SQLException {
		dbFile     = new File("/tmp/" + uuid() + ".sqlite");
		var conn   = new ConnectionProvider().get(dbFile);
		mailConfig = new SqliteMailConfig(conn);
	}
}
