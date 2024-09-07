/* © SRSoftware 2024 */
package de.srsoftware.oidc.datastore.sqlite;

import de.srsoftware.oidc.api.UserService;
import de.srsoftware.oidc.api.data.AccessToken;
import de.srsoftware.oidc.api.data.User;
import java.sql.Connection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class SqliteUserService implements UserService {
	private final Connection conn;

	public SqliteUserService(Connection connection) {
		conn = connection;
	}
	@Override
	public AccessToken accessToken(User user) {
		return null;
	}

	@Override
	public Optional<User> consumeToken(String accessToken) {
		return Optional.empty();
	}

	@Override
	public UserService delete(User user) {
		return null;
	}

	@Override
	public Optional<User> forToken(String accessToken) {
		return Optional.empty();
	}

	@Override
	public UserService init(User defaultUser) {
		return null;
	}

	@Override
	public List<User> list() {
		return List.of();
	}

	@Override
	public Set<User> find(String key) {
		return Set.of();
	}

	@Override
	public Optional<User> load(String id) {
		return Optional.empty();
	}

	@Override
	public Optional<User> load(String username, String password) {
		return Optional.empty();
	}

	@Override
	public boolean passwordMatches(String password, String hashedPassword) {
		return false;
	}

	@Override
	public <T extends UserService> T save(User user) {
		return null;
	}

	@Override
	public <T extends UserService> T updatePassword(User user, String plaintextPassword) {
		return null;
	}
}
