/* © SRSoftware 2024 */
package de.srsoftware.oidc.datastore.sqlite;

import static de.srsoftware.tools.Strings.uuid;
import static java.time.temporal.ChronoUnit.SECONDS;

import de.srsoftware.oidc.api.SessionService;
import de.srsoftware.oidc.api.data.Session;
import de.srsoftware.oidc.api.data.User;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Optional;

public class SqliteSessionService extends SqliteStore implements SessionService {
	private static final String STORE_VERSION	 = "session_store_version";
	private static final String CREATE_STORE_VERSION = "INSERT INTO metainfo (key,value) VALUES ('" + STORE_VERSION + "','0')";
	private static final String SELECT_STORE_VERSION = "SELECT * FROM metainfo WHERE key = '" + STORE_VERSION + "'";
	private static final String SET_STORE_VERSION	 = "UPDATE metainfo SET value = ? WHERE key = '" + STORE_VERSION + "'";

	private static final String CREATE_SESSION_TABLE = "CREATE TABLE sessions (id VARCHAR(64) PRIMARY KEY, userId VARCHAR(64) NOT NULL, expiration LONG NOT NULL, trust_browser BOOLEAN DEFAULT false)";
	private static final String SAVE_SESSION	 = "INSERT INTO sessions (id, userId, expiration, trust_browser) VALUES (?,?,?, ?) ON CONFLICT DO UPDATE SET expiration = ?, trust_browser = ?;";
	private static final String DROP_SESSION	 = "DELETE FROM sessions WHERE id = ?";
	private static final String SELECT_SESSION	 = "SELECT * FROM sessions WHERE id = ?";

	public SqliteSessionService(Connection connection) throws SQLException {
		super(connection);
	}

	@Override
	public Session createSession(User user, boolean trustBrowser) {
		var now	 = Instant.now();
		var endOfSession = now.plus(user.sessionDuration()).truncatedTo(SECONDS);
		return save(new Session(user.uuid(), endOfSession, uuid(), trustBrowser));
	}

	private void createStoreTables() throws SQLException {
		conn.prepareStatement(CREATE_SESSION_TABLE).execute();
	}

	@Override
	public SessionService dropSession(String sessionId) {
		try {
			var stmt = conn.prepareStatement(DROP_SESSION);
			stmt.setString(1, sessionId);
			stmt.execute();
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
		return this;
	}

	@Override
	public Session extend(Session session, User user) {
		var endOfSession = Instant.now().plus(user.sessionDuration());
		return save(new Session(user.uuid(), endOfSession, session.id(), session.trustBrowser()));
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
	public Optional<Session> retrieve(String sessionId) {
		try {
			var stmt = conn.prepareStatement(SELECT_SESSION);
			stmt.setString(1, sessionId);
			var	  rs     = stmt.executeQuery();
			Optional<Session> result = Optional.empty();
			if (rs.next()) {
				var userID	 = rs.getString("userId");
				var expiration	 = Instant.ofEpochSecond(rs.getLong("expiration"));
				var trustBrowser = rs.getBoolean("trust_browser");
				if (expiration.isAfter(Instant.now())) result = Optional.of(new Session(userID, expiration, sessionId, trustBrowser));
			}
			rs.close();
			return result;
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	private Session save(Session session) {
		try {
			var stmt       = conn.prepareStatement(SAVE_SESSION);
			var expiration = session.expiration().getEpochSecond();
			stmt.setString(1, session.id());
			stmt.setString(2, session.userId());
			stmt.setLong(3, expiration);
			stmt.setBoolean(4, session.trustBrowser());
			stmt.setLong(5, expiration);
			stmt.setBoolean(6, session.trustBrowser());
			stmt.execute();
			return session;
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}
}
