/* © SRSoftware 2024 */
package de.srsoftware.oidc.backend;

import com.sun.net.httpserver.HttpExchange;
import de.srsoftware.http.PathHandler;
import de.srsoftware.http.SessionToken;
import de.srsoftware.oidc.api.SessionService;
import de.srsoftware.oidc.api.UserService;
import de.srsoftware.oidc.api.data.Session;
import java.util.Optional;

public abstract class Controller extends PathHandler {
	protected final SessionService sessions;
	private final UserService      users;

	Controller(SessionService sessionService, UserService userService) {
		sessions = sessionService;
		users    = userService;
	}

	protected Optional<Session> getSession(HttpExchange ex) {
		return SessionToken.from(ex).map(SessionToken::sessionId).flatMap(sessionId -> sessions.retrieve(sessionId, users));
	}
}
