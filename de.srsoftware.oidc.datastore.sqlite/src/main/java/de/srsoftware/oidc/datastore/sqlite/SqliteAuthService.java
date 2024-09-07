/* © SRSoftware 2024 */
package de.srsoftware.oidc.datastore.sqlite;

import de.srsoftware.oidc.api.AuthorizationService;
import de.srsoftware.oidc.api.data.AuthResult;
import de.srsoftware.oidc.api.data.Authorization;
import de.srsoftware.oidc.api.data.Client;
import de.srsoftware.oidc.api.data.User;
import java.sql.Connection;
import java.time.Instant;
import java.util.Collection;
import java.util.Optional;

public class SqliteAuthService implements AuthorizationService {
	public SqliteAuthService(Connection connection) {
	}

	@Override
	public AuthorizationService authorize(User user, Client client, Collection<String> scopes, Instant expiration) {
		return null;
	}

	@Override
	public Optional<Authorization> consumeAuthorization(String authCode) {
		return Optional.empty();
	}

	@Override
	public AuthResult getAuthorization(User user, Client client, Collection<String> scopes) {
		return null;
	}
}
