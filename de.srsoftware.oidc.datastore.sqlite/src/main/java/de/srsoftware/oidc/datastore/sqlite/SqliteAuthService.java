/* © SRSoftware 2024 */
package de.srsoftware.oidc.datastore.sqlite;

import static de.srsoftware.utils.Optionals.nullable;
import static de.srsoftware.utils.Strings.uuid;

import de.srsoftware.oidc.api.AuthorizationService;
import de.srsoftware.oidc.api.data.AuthResult;
import de.srsoftware.oidc.api.data.Authorization;
import de.srsoftware.oidc.api.data.AuthorizedScopes;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

public class SqliteAuthService extends SqliteStore implements AuthorizationService {
	private static final String STORE_VERSION	 = "auth_store_version";
	private static final String CREATE_STORE_VERSION = "INSERT INTO metainfo (key,value) VALUES ('" + STORE_VERSION + "','0')";
	private static final String SELECT_STORE_VERSION = "SELECT * FROM metainfo WHERE key = '" + STORE_VERSION + "'";
	private static final String SET_STORE_VERSION	 = "UPDATE metainfo SET value = ? WHERE key = '" + STORE_VERSION + "'";

	private static final String	   CREATE_AUTHSTORE_TABLE = "CREATE TABLE IF NOT EXISTS authorizations(userId VARCHAR(255), clientId VARCHAR(255), scope VARCHAR(255), expiration LONG, PRIMARY KEY(userId, clientId, scope));";
	private static final String	   SAVE_AUTHORIZATION     = "INSERT INTO authorizations(userId, clientId, scope, expiration) VALUES (?,?,?,?) ON CONFLICT DO UPDATE SET expiration = ?";
	private static final String	   SELECT_AUTH	          = "SELECT * FROM authorizations WHERE userId = ? AND clientId = ? AND scope IN";
	private static final String	   SELECT_USER_CLIENTS    = "SELECT DISTINCT clientId FROM authorizations WHERE userId = ?";
	private Map<String, Authorization> authCodes	          = new HashMap<>();

	private Map<String, String> nonceMap = new HashMap<>();

	public SqliteAuthService(Connection connection) throws SQLException {
		super(connection);
	}

	private void createStoreTables() throws SQLException {
		conn.prepareStatement(CREATE_AUTHSTORE_TABLE).execute();
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

	private String authCode(Authorization authorization) {
		var code = uuid();
		authCodes.put(code, authorization);
		return code;
	}

	@Override
	public AuthorizationService authorize(String userId, String clientId, Collection<String> scopes, Instant expiration) {
		try {
			conn.setAutoCommit(false);
			var stmt = conn.prepareStatement(SAVE_AUTHORIZATION);
			stmt.setString(1, userId);
			stmt.setString(2, clientId);
			stmt.setLong(4, expiration.getEpochSecond());
			stmt.setLong(5, expiration.getEpochSecond());
			for (var scope : scopes) {
				stmt.setString(3, scope);
				stmt.execute();
			}
			conn.commit();
			conn.setAutoCommit(true);
			return this;
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public List<String> authorizedClients(String userId) {
		try {
			var stmt = conn.prepareStatement(SELECT_USER_CLIENTS);
			stmt.setString(1, userId);
			var rs     = stmt.executeQuery();
			var result = new ArrayList<String>();
			while (rs.next()) result.add(rs.getString(1));
			rs.close();
			return result;
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public Optional<Authorization> consumeAuthorization(String authCode) {
		return nullable(authCodes.remove(authCode));
	}

	@Override
	public Optional<String> consumeNonce(String userId, String clientId) {
		var nonceKey = String.join("@", userId, clientId);
		return nullable(nonceMap.get(nonceKey));
	}

	@Override
	public AuthResult getAuthorization(String userId, String clientId, Collection<String> scopes) {
		try {
			var scopeList = "(" + scopes.stream().map(s -> "?").collect(Collectors.joining(", ")) + ")";
			var sql       = SELECT_AUTH + scopeList;
			var stmt      = conn.prepareStatement(sql);
			stmt.setString(1, userId);
			stmt.setString(2, clientId);
			int i = 3;
			for (var scope : scopes) stmt.setString(i++, scope);
			var     rs	     = stmt.executeQuery();
			var     unauthorized = new HashSet<String>(scopes);
			var     authorized   = new HashSet<String>();
			var     now	     = Instant.now();
			Instant earliestExp  = null;
			while (rs.next()) {
				long    expiration = rs.getLong("expiration");
				String  scope	   = rs.getString("scope");
				Instant ex	   = Instant.ofEpochSecond(expiration).truncatedTo(ChronoUnit.SECONDS);
				if (ex.isAfter(now)) {
					unauthorized.remove(scope);
					authorized.add(scope);
					if (earliestExp == null || ex.isBefore(earliestExp)) earliestExp = ex;
				}
			}
			rs.close();
			if (authorized.isEmpty()) return new AuthResult(null, unauthorized, null);
			var authorizedScopes = new AuthorizedScopes(authorized, earliestExp);
			var authorization    = new Authorization(clientId, userId, authorizedScopes);
			return new AuthResult(authorizedScopes, unauthorized, authCode(authorization));
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void nonce(String userId, String clientId, String nonce) {
		var nonceKey = String.join("@", userId, clientId);
		if (nonce != null) {
			nonceMap.put(nonceKey, nonce);
		} else
			nonceMap.remove(nonceKey);
	}
}
