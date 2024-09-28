/* © SRSoftware 2024 */
package de.srsoftware.oidc.datastore.sqlite;


import de.srsoftware.oidc.api.KeyStorage;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.jose4j.jwk.PublicJsonWebKey;

public class SqliteKeyStore extends SqliteStore implements KeyStorage {
	private static final String STORE_VERSION	 = "key_store_version";
	private static final String CREATE_STORE_VERSION = "INSERT INTO metainfo (key,value) VALUES ('" + STORE_VERSION + "','0')";
	private static final String SELECT_STORE_VERSION = "SELECT * FROM metainfo WHERE key = '" + STORE_VERSION + "'";
	private static final String SET_STORE_VERSION	 = "UPDATE metainfo SET value = ? WHERE key = '" + STORE_VERSION + "'";

	private static final String SET_KEYSTORE_VERSION  = "UPDATE metainfo SET value = ? WHERE key = 'key_store_version'";
	private static final String CREATE_KEYSTORE_TABLE = "CREATE TABLE IF NOT EXISTS keystore(key_id VARCHAR(255) PRIMARY KEY, json TEXT NOT NULL);";
	private static final String SAVE_KEY	  = "INSERT INTO keystore(key_id, json) values (?,?) ON CONFLICT(key_id) DO UPDATE SET json = ?";
	private static final String SELECT_KEY_IDS	  = "SELECT key_id FROM keystore";
	private static final String LOAD_KEY	  = "SELECT json FROM keystore WHERE key_id = ?";
	private static final String DROP_KEY	  = "DELETE FROM keystore WHERE key_id = ?";

	private HashMap<String, PublicJsonWebKey> loaded = new HashMap<>();

	public SqliteKeyStore(Connection connection) throws SQLException {
		super(connection);
	}

	private void createStoreTables() throws SQLException {
		conn.prepareStatement(CREATE_KEYSTORE_TABLE).execute();
	}

	@Override
	public KeyStorage drop(String keyId) {
		try {
			var stmt = conn.prepareStatement(DROP_KEY);
			stmt.setString(1, keyId);
			stmt.execute();
		} catch (SQLException e) {
			LOG.log(System.Logger.Level.WARNING, "Failed to drop key {0} from database:", keyId, e);
		}
		return this;
	}

	@Override
	protected void initTables() throws SQLException {
		var rs	     = conn.prepareStatement(SELECT_STORE_VERSION).executeQuery();
		int availableVersion = 1;
		int currentVersion;
		if (rs.next()) {
			currentVersion = rs.getInt("value");
			rs.close();
		} else {
			rs.close();
			conn.prepareStatement(CREATE_STORE_VERSION).execute();
			currentVersion = 0;
		}

		conn.setAutoCommit(false);
		var stmt = conn.prepareStatement(SET_STORE_VERSION);
		while (currentVersion < availableVersion) {
			try {
				switch (currentVersion) {
					case 0:
						createStoreTables();
						break;
				}
				stmt.setInt(1, ++currentVersion);
				stmt.execute();
				conn.commit();
			} catch (Exception e) {
				conn.rollback();
				LOG.log(System.Logger.Level.ERROR, "Failed to update at {} = {}", STORE_VERSION, currentVersion);
				break;
			}
		}
		conn.setAutoCommit(true);
	}

	@Override
	public List<String> listKeys() {
		var result = new ArrayList<String>();
		try {
			var rs = conn.prepareStatement(SELECT_KEY_IDS).executeQuery();
			while (rs.next()) result.add(rs.getString(1));
			rs.close();
		} catch (SQLException e) {
			LOG.log(System.Logger.Level.WARNING, "Failed to read key ids from table!");
		}
		return result;
	}

	@Override
	public String loadJson(String keyId) throws IOException {
		try {
			var stmt = conn.prepareStatement(LOAD_KEY);
			stmt.setString(1, keyId);
			var    rs   = stmt.executeQuery();
			String json = null;
			if (rs.next()) json = rs.getString(1);
			rs.close();
			return json;
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public KeyStorage store(String keyId, String json) throws IOException {
		try {
			var stmt = conn.prepareStatement(SAVE_KEY);
			stmt.setString(1, keyId);
			stmt.setString(2, json);
			stmt.setString(3, json);
			stmt.execute();
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
		return this;
	}
}
