/* © SRSoftware 2024 */
package de.srsoftware.oidc.datastore.sqlite;


import static org.jose4j.jwk.JsonWebKey.OutputControlLevel.INCLUDE_PRIVATE;

import de.srsoftware.oidc.api.KeyManager;
import de.srsoftware.oidc.api.KeyStorage;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.jose4j.jwk.PublicJsonWebKey;
import org.jose4j.lang.JoseException;

public class SqliteKeyStore implements KeyStorage {
	private static final String CREATE_MIGRATION_TABLE  = "CREATE TABLE IF NOT EXISTS metainfo(key VARCHAR(255) PRIMARY KEY, value TEXT);";
	private static final String SELECT_KEYSTORE_VERSION = "SELECT * FROM metainfo WHERE key = 'key_store_version'";
	private static final String SET_KEYSTORE_VERSION    = "UPDATE metainfo SET value = ? WHERE key = 'key_store_version'";
	private static final String CREATE_KEYSTORE_TABLE   = "CREATE TABLE IF NOT EXISTS keystore(key_id VARCHAR(255) PRIMARY KEY, json TEXT NOT NULL);";
	private static final String SAVE_KEY	    = "INSERT INTO keystore(key_id, json) values (?,?) ON CONFLICT(key_id) DO UPDATE SET json = ?";
	private static final String SELECT_KEY_IDS	    = "SELECT key_id FROM keystore";
	private static final String LOAD_KEY	    = "SELECT json FROM keystore WHERE key_id = ?";
	private static final String DROP_KEY	    = "DELETE FROM keystore WHERE key_id = ?";
	public static System.Logger LOG		    = System.getLogger(SqliteKeyStore.class.getSimpleName());

	private HashMap<String, PublicJsonWebKey> loaded = new HashMap<>();
	private final Connection	          conn;

	public SqliteKeyStore(Connection connection) throws SQLException {
		conn = connection;
		initTables();
	}

	private void initTables() throws SQLException {
		conn.prepareStatement(CREATE_MIGRATION_TABLE).execute();
		var rs	     = conn.prepareStatement(SELECT_KEYSTORE_VERSION).executeQuery();
		int availableVersion = 1;
		int lastVersion	     = 1;
		if (rs.next()) {
			lastVersion = rs.getInt(1);
		}
		rs.close();
		conn.setAutoCommit(false);
		var stmt = conn.prepareStatement(SET_KEYSTORE_VERSION);
		for (int version = lastVersion; version <= availableVersion; version++) {
			try {
				switch (version) {
					case 1:
						createKeyStoreTables();
				}
				stmt.setInt(1, version);
				stmt.execute();
				conn.commit();
			} catch (Exception e) {
				conn.rollback();
				LOG.log(System.Logger.Level.ERROR, "Failed to update at keystore version = {0}", version);
				break;
			}
		}
		conn.setAutoCommit(true);
	}

	private void createKeyStoreTables() throws SQLException {
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
	public PublicJsonWebKey load(String keyId) throws IOException, KeyManager.KeyCreationException {
		try {
			var stmt = conn.prepareStatement(LOAD_KEY);
			stmt.setString(1, keyId);
			var    rs   = stmt.executeQuery();
			String json = null;
			if (rs.next()) {
				json = rs.getString(1);
			}
			rs.close();
			return PublicJsonWebKey.Factory.newPublicJwk(json);
		} catch (JoseException e) {
			throw new KeyManager.KeyCreationException(e);
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public KeyStorage store(PublicJsonWebKey jsonWebKey) throws IOException {
		try {
			var keyId = jsonWebKey.getKeyId();
			var json  = jsonWebKey.toJson(INCLUDE_PRIVATE);
			var stmt  = conn.prepareStatement(SAVE_KEY);
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
