/* © SRSoftware 2024 */
package de.srsoftware.oidc.datastore.sqlite;

import de.srsoftware.oidc.api.SessionService;
import de.srsoftware.oidc.api.UserService;
import de.srsoftware.oidc.api.data.Session;
import de.srsoftware.oidc.api.data.User;
import java.sql.Connection;
import java.time.Duration;
import java.util.Optional;

public class SqliteSessionService implements SessionService {
	public SqliteSessionService(Connection connection) {
	}

	@Override
	public Session createSession(User user) {
		return null;
	}

	@Override
	public SessionService dropSession(String sessionId) {
		return null;
	}

	@Override
	public Session extend(Session session) {
		return null;
	}

	@Override
	public Optional<Session> retrieve(String sessionId, UserService users) {
		return Optional.empty();
	}

	@Override
	public SessionService setDuration(Duration duration) {
		return null;
	}
}
