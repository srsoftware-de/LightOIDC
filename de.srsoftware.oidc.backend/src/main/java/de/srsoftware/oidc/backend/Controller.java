/* © SRSoftware 2024 */
package de.srsoftware.oidc.backend;

import com.sun.net.httpserver.HttpExchange;
import de.srsoftware.cookies.SessionToken;
import de.srsoftware.oidc.api.PathHandler;
import de.srsoftware.oidc.api.Session;
import de.srsoftware.oidc.api.SessionService;
import java.util.Optional;

public abstract class Controller extends PathHandler {
	protected final SessionService sessions;

	Controller(SessionService sessionService) {
		sessions = sessionService;
	}

	protected Optional<Session> getSession(HttpExchange ex) {
		return SessionToken.from(ex).map(SessionToken::sessionId).flatMap(sessions::retrieve);
	}
}
