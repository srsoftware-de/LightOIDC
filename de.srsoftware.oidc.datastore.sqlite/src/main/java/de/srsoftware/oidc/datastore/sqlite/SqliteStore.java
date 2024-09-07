/* © SRSoftware 2024 */
package de.srsoftware.oidc.datastore.sqlite;

import java.sql.Connection;
import java.sql.SQLException;

public abstract class SqliteStore {
	public static System.Logger LOG		   = System.getLogger(SqliteStore.class.getSimpleName());
	private static final String CREATE_MIGRATION_TABLE = "CREATE TABLE IF NOT EXISTS metainfo(key VARCHAR(255) PRIMARY KEY, value TEXT);";

	protected final Connection conn;

	public SqliteStore(Connection connection) throws SQLException {
		conn = connection;
		conn.prepareStatement(CREATE_MIGRATION_TABLE).execute();
		initTables();
	}

	protected abstract void initTables() throws SQLException;
}
