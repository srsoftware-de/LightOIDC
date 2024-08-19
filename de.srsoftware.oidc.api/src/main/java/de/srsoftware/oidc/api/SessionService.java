/* © SRSoftware 2024 */
package de.srsoftware.oidc.api;

import de.srsoftware.oidc.api.data.Session;
import de.srsoftware.oidc.api.data.User;
import java.time.Duration;
import java.util.Optional;

public interface SessionService {
	Session	  createSession(User user);
	SessionService	  dropSession(String sessionId);
	Session	  extend(Session session);
	Optional<Session> retrieve(String sessionId);
	SessionService	  setDuration(Duration duration);
}
