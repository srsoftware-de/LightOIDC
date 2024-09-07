/* © SRSoftware 2024 */
package de.srsoftware.oidc.datastore.sqlite;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import org.sqlite.SQLiteDataSource;

public class ConnectionProvider extends HashMap<File, Connection> {
	public Connection get(Object o) {
		if (o instanceof File dbFile) try {
				var conn = super.get(dbFile);
				if (conn == null) put(dbFile, conn = open(dbFile));
				return conn;
			} catch (SQLException sqle) {
				throw new RuntimeException(sqle);
			}
		return null;
	}

	private Connection open(File dbFile) throws SQLException {
		SQLiteDataSource dataSource = new SQLiteDataSource();
		dataSource.setUrl("jdbc:sqlite:%s".formatted(dbFile));
		return dataSource.getConnection();
	}
}
