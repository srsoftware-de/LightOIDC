/* © SRSoftware 2024 */
package de.srsoftware.oidc.datastore.sqlite;

import static de.srsoftware.utils.Optionals.nullable;
import static de.srsoftware.utils.Strings.uuid;
import static java.util.Optional.empty;

import de.srsoftware.oidc.api.PasswordHasher;
import de.srsoftware.oidc.api.UserService;
import de.srsoftware.oidc.api.data.AccessToken;
import de.srsoftware.oidc.api.data.Permission;
import de.srsoftware.oidc.api.data.User;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

public class SqliteUserService extends SqliteStore implements UserService {
	private static final String CREATE_USER_TABLE	         = "CREATE TABLE IF NOT EXISTS users(uuid VARCHAR(255) NOT NULL PRIMARY KEY, password VARCHAR(255), email VARCHAR(255), session_duration INT NOT NULL DEFAULT 10, username VARCHAR(255), realname VARCHAR(255));";
	private static final String CREATE_USER_PERMISSION_TABLE = "CREATE TABLE IF NOT EXISTS user_permissions(uuid VARCHAR(255), permission VARCHAR(50), PRIMARY KEY(uuid,permission));";
	private static final String COUNT_USERS	         = "SELECT count(*) FROM users";
	private static final String LOAD_USER	         = "SELECT * FROM users WHERE uuid = ?";
	private static final String LOAD_PERMISSIONS	         = "SELECT permission FROM user_permissions WHERE uuid = ?";
	private static final String FIND_USER	         = "SELECT * FROM users WHERE uuid = ? OR username LIKE ? OR realname LIKE ? ORDER BY COALESCE(uuid, ?), username";
	private static final String LIST_USERS	         = "SELECT * FROM users";
	private static final String SELECT_USERSTORE_VERSION     = "SELECT * FROM metainfo WHERE key = 'user_store_version'";
	private static final String SET_USERSTORE_VERSION        = "UPDATE metainfo SET value = ? WHERE key = 'user_store_version'";
	private static final String INSERT_USER	         = "INSERT INTO users (uuid,password,email,session_duration,username,realname) VALUES (?,?,?,?,?,?)";
	private static final String INSERT_PERMISSIONS	         = "INSERT INTO user_permissions (uuid, permission) VALUES (?,?)";

	private static final String DROP_PERMISSIONS = "DELETE FROM user_permissions WHERE uuid = ?";
	private static final String DROP_USER        = "DELETE FROM users WHERE uuid = ?";
	private static final String UPDATE_PASSWORD  = "UPDATE users SET password = ? WHERE uuid = ?";
	private final PasswordHasher<String> hasher;

	private Map<String, AccessToken> accessTokens = new HashMap<>();


	public SqliteUserService(Connection connection, PasswordHasher<String> passHasher) throws SQLException {
		super(connection);
		hasher = passHasher;
	}

	@Override
	public AccessToken accessToken(User user) {
		var token = new AccessToken(uuid(), Objects.requireNonNull(user), Instant.now().plus(1, ChronoUnit.HOURS));
		accessTokens.put(token.id(), token);
		return token;
	}

	@Override
	public Optional<User> consumeToken(String id) {
		var user = forToken(id);
		accessTokens.remove(id);
		return user;
	}

	private void createUserStoreTables() throws SQLException {
		conn.prepareStatement(CREATE_USER_TABLE).execute();
		conn.prepareStatement(CREATE_USER_PERMISSION_TABLE).execute();
	}

	@Override
	public UserService delete(User user) {
		try {
			conn.setAutoCommit(false);
			dropPermissionsOf(user.uuid());
			var stmt = conn.prepareStatement(DROP_USER);
			stmt.setString(1, user.uuid());
			stmt.execute();
			conn.commit();
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
		return this;
	}

	private void dropPermissionsOf(String uuid) throws SQLException {
		var stmt = conn.prepareStatement(DROP_PERMISSIONS);
		stmt.setString(1, uuid);
		stmt.execute();
	}

	@Override
	public Optional<User> forToken(String id) {
		AccessToken token = accessTokens.get(id);
		if (token == null) return empty();
		if (token.valid()) return Optional.of(token.user());
		accessTokens.remove(id);
		return empty();
	}

	@Override
	public UserService init(User defaultUser) {
		try {
			var rs    = conn.prepareStatement(COUNT_USERS).executeQuery();
			var count = rs.next() ? rs.getInt(1) : 0;
			rs.close();
			if (count < 1) save(defaultUser);
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
		return this;
	}

	@Override
	protected void initTables() throws SQLException {
		var rs	     = conn.prepareStatement(SELECT_USERSTORE_VERSION).executeQuery();
		int availableVersion = 1;
		int lastVersion	     = 1;
		if (rs.next()) {
			lastVersion = rs.getInt(1);
		}
		rs.close();
		conn.setAutoCommit(false);
		var stmt = conn.prepareStatement(SET_USERSTORE_VERSION);
		for (int version = lastVersion; version <= availableVersion; version++) {
			try {
				switch (version) {
					case 1:
						createUserStoreTables();
				}
				stmt.setInt(1, version);
				stmt.execute();
				conn.commit();
			} catch (Exception e) {
				conn.rollback();
				LOG.log(System.Logger.Level.ERROR, "Failed to update at user store version = {0}", version);
				break;
			}
		}
		conn.setAutoCommit(true);
	}


	@Override
	public List<User> list() {
		try {
			List<User> result = new ArrayList<>();
			var        rs	  = conn.prepareStatement(LIST_USERS).executeQuery();
			while (rs.next()) result.add(userFrom(rs));
			rs.close();
			for (User user : result) addPermissions(user);
			return result;
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	private User userFrom(ResultSet rs) throws SQLException {
		var uuid = rs.getString("uuid");
		var pass = rs.getString("password");
		var user = rs.getString("username");
		var name = rs.getString("realname");
		var mail = rs.getString("email");
		var mins = rs.getLong("session_duration");
		return new User(user, pass, name, mail, uuid).sessionDuration(Duration.ofMinutes(mins));
	}

	@Override
	public Set<User> find(String idOrEmail) {
		try {
			var result = new HashSet<User>();
			var stmt   = conn.prepareStatement(FIND_USER);	// TODO: implement test for this query
			stmt.setString(1, idOrEmail);
			stmt.setString(2, "%" + idOrEmail + "%");
			stmt.setString(3, "%" + idOrEmail + "%");
			stmt.setString(4, idOrEmail);
			var rs = stmt.executeQuery();
			while (rs.next()) result.add(userFrom(rs));
			rs.close();
			return result;
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public Optional<User> load(String id) {
		try {
			User user = null;
			var  stmt = conn.prepareStatement(LOAD_USER);
			stmt.setString(1, id);
			var rs = stmt.executeQuery();
			if (rs.next()) user = userFrom(rs);
			rs.close();
			return nullable(user).map(this::addPermissions);
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	private User addPermissions(User user) {
		try {
			var stmt = conn.prepareStatement(LOAD_PERMISSIONS);
			stmt.setString(1, user.uuid());
			var rs = stmt.executeQuery();
			while (rs.next()) try {
					user.add(Permission.valueOf(rs.getString("permission")));
				} catch (IllegalArgumentException ignored) {
				}
			rs.close();
			return user;
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public Optional<User> load(String username, String password) {
		var candidates = find(username);
		for (var user : candidates) {
			if (passwordMatches(password, user.hashedPassword())) return Optional.of(user);
		}
		return empty();
	}

	@Override
	public boolean passwordMatches(String password, String hashedPassword) {
		return hasher.matches(password, hashedPassword);
	}

	@Override
	public SqliteUserService save(User user) {
		try {
			conn.setAutoCommit(false);
			var stmt = conn.prepareStatement(INSERT_USER);
			stmt.setString(1, user.uuid());
			stmt.setString(2, user.hashedPassword());
			stmt.setString(3, user.email());
			stmt.setLong(4, user.sessionDuration().toMinutes());
			stmt.setString(5, user.username());
			stmt.setString(6, user.realName());
			stmt.execute();
			dropPermissionsOf(user.uuid());

			stmt = conn.prepareStatement(INSERT_PERMISSIONS);
			stmt.setString(1, user.uuid());
			for (Permission perm : Permission.values()) {
				if (user.hasPermission(perm)) {
					stmt.setString(2, perm.toString());
					stmt.execute();
				}
			}
			conn.commit();

		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
		return this;
	}

	@Override
	public SqliteUserService updatePassword(User user, String plaintextPassword) {
		try {
			var stmt = conn.prepareStatement(UPDATE_PASSWORD);
			stmt.setString(1, user.uuid());
			var hashedPassword = hasher.hash(plaintextPassword, uuid());
			stmt.setString(2, hashedPassword);
			stmt.execute();
			return this;
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}
}
