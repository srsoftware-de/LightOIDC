/* © SRSoftware 2025 */
package de.srsoftware.oidc.api;

import de.srsoftware.oidc.api.data.Session;
import de.srsoftware.oidc.api.data.User;
import java.util.Optional;

public interface SessionService {
	Session	  createSession(User user, boolean trustBrowser);
	SessionService	  dropSession(String sessionId);
	Session	  extend(Session session, User user);
	Optional<Session> retrieve(String sessionId);
}
