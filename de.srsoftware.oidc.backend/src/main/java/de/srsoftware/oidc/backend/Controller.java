/* © SRSoftware 2024 */
package de.srsoftware.oidc.backend;

import com.sun.net.httpserver.HttpExchange;
import de.srsoftware.http.PathHandler;
import de.srsoftware.http.SessionToken;
import de.srsoftware.oidc.api.SessionService;
import de.srsoftware.oidc.api.data.Session;
import java.io.IOException;
import java.util.Optional;

public abstract class Controller extends PathHandler {
	protected final SessionService sessions;

	Controller(SessionService sessionService) {
		sessions = sessionService;
	}

	protected Optional<Session> getSession(HttpExchange ex) {
		return SessionToken.from(ex).map(SessionToken::sessionId).flatMap(sessionId -> sessions.retrieve(sessionId));
	}

	protected boolean invalidSessionUser(HttpExchange ex) throws IOException {
		return serverError(ex, "Session object refers to missing user");
	}
}
