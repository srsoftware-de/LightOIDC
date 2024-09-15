/* © SRSoftware 2024 */
package de.srsoftware.oidc.datastore.sqlite;

import static de.srsoftware.oidc.api.Constants.NAME;
import static de.srsoftware.oidc.api.Constants.SECRET;

import de.srsoftware.oidc.api.ClientService;
import de.srsoftware.oidc.api.data.Client;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

public class SqliteClientService extends SqliteStore implements ClientService {
	private static final String STORE_VERSION	 = "client_store_version";
	private static final String CREATE_STORE_VERSION = "INSERT INTO metainfo (key,value) VALUES ('" + STORE_VERSION + "','0')";
	private static final String SELECT_STORE_VERSION = "SELECT * FROM metainfo WHERE key = '" + STORE_VERSION + "'";
	private static final String SET_STORE_VERSION	 = "UPDATE metainfo SET value = ? WHERE key = '" + STORE_VERSION + "'";

	private static final String CREATE_CLIENT_TABLE	    = "CREATE TABLE IF NOT EXISTS clients(id VARCHAR(255) NOT NULL PRIMARY KEY, name VARCHAR(255), secret VARCHAR(255));";
	private static final String CREATE_REDIRECT_TABLE   = "CREATE TABLE IF NOT EXISTS client_redirects(clientId VARCHAR(255), uri VARCHAR(255), PRIMARY KEY(clientId, uri));";
	private static final String SAVE_CLIENT	    = "INSERT INTO clients (id, name, secret) VALUES (?,?,?) ON CONFLICT DO UPDATE SET name = ?, secret = ?;";
	private static final String SAVE_REDIRECT	    = "INSERT OR IGNORE INTO client_redirects(clientId, uri) VALUES (?, ?)";
	private static final String DROP_OTHER_REDIRECTS    = "DELETE FROM client_redirects WHERE clientId = ? AND uri NOT IN";
	private static final String SELECT_CLIENT	    = "SELECT * FROM clients WHERE id = ?";
	private static final String SELECT_CLIENT_REDIRECTS = "SELECT uri FROM client_redirects WHERE clientId = ?";
	private static final String LIST_CLIENT_REDIRECTS   = "SELECT * FROM client_redirects";
	private static final String LIST_CLIENTS	    = "SELECT * FROM clients";
	private static final String DELETE_CLIENT	    = "DELETE FROM clients WHERE id = ?";
	private static final String DELETE_CLIENT_REDIRECTS = "DELETE FROM client_redirects WHERE clientId = ?";

	public SqliteClientService(Connection connection) throws SQLException {
		super(connection);
	}

	private void createStoreTables() throws SQLException {
		conn.prepareStatement(CREATE_CLIENT_TABLE).execute();
		conn.prepareStatement(CREATE_REDIRECT_TABLE).execute();
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
	public Optional<Client> getClient(String clientId) {
		Optional<Client> result = Optional.empty();
		try {
			var stmt = conn.prepareStatement(SELECT_CLIENT_REDIRECTS);
			stmt.setString(1, clientId);
			var rs   = stmt.executeQuery();
			var uris = new HashSet<String>();
			while (rs.next()) uris.add(rs.getString("uri"));
			rs.close();
			stmt = conn.prepareStatement(SELECT_CLIENT);
			stmt.setString(1, clientId);
			rs = stmt.executeQuery();
			if (rs.next()) {
				var name   = rs.getString(NAME);
				var secret = rs.getString(SECRET);
				result     = Optional.of(new Client(clientId, name, secret, uris));
			}
			rs.close();
			return result;
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public List<Client> listClients() {
		try {
			var stmt      = conn.prepareStatement(LIST_CLIENT_REDIRECTS);
			var rs        = stmt.executeQuery();
			var redirects = new HashMap<String, Set<String>>();
			while (rs.next()) {
				var clientId = rs.getString("clientId");
				var uri      = rs.getString("uri");
				var set      = redirects.computeIfAbsent(clientId, k -> new HashSet<>());
				set.add(uri);
			}
			rs.close();
			stmt       = conn.prepareStatement(LIST_CLIENTS);
			rs         = stmt.executeQuery();
			var result = new ArrayList<Client>();
			while (rs.next()) {
				var id     = rs.getString("id");
				var name   = rs.getString(NAME);
				var secret = rs.getString(SECRET);
				result.add(new Client(id, name, secret, redirects.get(id)));
			}
			rs.close();
			return result;
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public ClientService remove(String clientId) {
		try {
			var stmt = conn.prepareStatement(DELETE_CLIENT);
			stmt.setString(1, clientId);
			stmt.execute();
			stmt = conn.prepareStatement(DELETE_CLIENT_REDIRECTS);
			stmt.setString(1, clientId);
			stmt.execute();
			return this;
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public ClientService save(Client client) {
		try {
			var stmt = conn.prepareStatement(SAVE_CLIENT);
			stmt.setString(1, client.id());
			stmt.setString(2, client.name());
			stmt.setString(3, client.secret());
			stmt.setString(4, client.name());
			stmt.setString(5, client.secret());
			stmt.execute();
			stmt = conn.prepareStatement(SAVE_REDIRECT);
			stmt.setString(1, client.id());
			for (var redirect : client.redirectUris()) {
				stmt.setString(2, redirect);
				stmt.execute();
			}
			var where = "(" + client.redirectUris().stream().map(u -> "?").collect(Collectors.joining(", ")) + ")";
			var sql   = DROP_OTHER_REDIRECTS + where;
			stmt      = conn.prepareStatement(sql);
			stmt.setString(1, client.id());
			int i = 2;
			for (var redirect : client.redirectUris()) stmt.setString(i++, redirect);
			stmt.execute();
			return this;
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}
}
