/* © SRSoftware 2024 */
package de.srsoftware.oidc.datastore.sqlite;

import static de.srsoftware.utils.Strings.uuid;
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

	private static final String CREATE_SESSION_TABLE = "CREATE TABLE sessions (id VARCHAR(64) PRIMARY KEY, userId VARCHAR(64) NOT NULL, expiration LONG NOT NULL)";

	public SqliteSessionService(Connection connection) throws SQLException {
		super(connection);
	}

	@Override
	protected void initTables() throws SQLException {
		var rs	     = conn.prepareStatement(SELECT_STORE_VERSION).executeQuery();
		int availableVersion = 1;
		int currentVersion;
		if (rs.next()) {
			currentVersion = rs.getInt(1);
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

	private void createStoreTables() throws SQLException {
		conn.prepareStatement(CREATE_SESSION_TABLE).execute();
	}

	@Override
	public Session createSession(User user) {
		var now	 = Instant.now();
		var endOfSession = now.plus(user.sessionDuration()).truncatedTo(SECONDS);
		return save(new Session(user.uuid(), endOfSession, uuid()));
	}

	@Override
	public SessionService dropSession(String sessionId) {
		return null;
	}

	@Override
	public Session extend(Session session, User user) {
		return null;
	}

	@Override
	public Optional<Session> retrieve(String sessionId) {
		return Optional.empty();
	}

	private Session save(Session session) {
		return session;
	}
}
