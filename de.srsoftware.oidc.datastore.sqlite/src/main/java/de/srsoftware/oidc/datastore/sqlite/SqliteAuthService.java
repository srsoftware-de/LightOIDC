/* © SRSoftware 2024 */
package de.srsoftware.oidc.datastore.sqlite;

import de.srsoftware.oidc.api.AuthorizationService;
import de.srsoftware.oidc.api.data.AuthResult;
import de.srsoftware.oidc.api.data.Authorization;
import java.sql.Connection;
import java.time.Instant;
import java.util.Collection;
import java.util.Optional;

public class SqliteAuthService implements AuthorizationService {
	public SqliteAuthService(Connection connection) {
	}

	@Override
	public AuthorizationService authorize(String userId, String clientId, Collection<String> scopes, Instant expiration) {
		return null;
	}

	@Override
	public Optional<Authorization> consumeAuthorization(String authCode) {
		return Optional.empty();
	}

	@Override
	public AuthResult getAuthorization(String userId, String clientId, Collection<String> scopes) {
		return null;
	}
}
