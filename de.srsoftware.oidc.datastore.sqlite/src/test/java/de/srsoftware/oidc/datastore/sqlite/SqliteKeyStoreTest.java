/* © SRSoftware 2024 */
package de.srsoftware.oidc.datastore.sqlite;

import static de.srsoftware.utils.Strings.uuid;

import de.srsoftware.oidc.api.KeyStorage;
import de.srsoftware.oidc.api.KeyStoreTest;
import java.io.File;
import java.sql.SQLException;
import org.junit.jupiter.api.BeforeEach;

public class SqliteKeyStoreTest extends KeyStoreTest {
	private KeyStorage keyStore;

	@Override
	protected KeyStorage keyStore() {
		return keyStore;
	}

	@BeforeEach
	public void setup() throws SQLException {
		var dbFile = new File("/tmp/" + uuid() + ".sqlite");
		var conn   = new ConnectionProvider().get(dbFile);
		keyStore   = new SqliteKeyStore(conn);
	}
}
