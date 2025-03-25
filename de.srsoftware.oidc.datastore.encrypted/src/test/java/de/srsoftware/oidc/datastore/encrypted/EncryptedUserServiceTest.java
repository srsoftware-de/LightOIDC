/* © SRSoftware 2025 */
package de.srsoftware.oidc.datastore.encrypted; /* © SRSoftware 2024 */
import static de.srsoftware.oidc.api.Constants.*;
import static de.srsoftware.tools.Optionals.nullable;
import static de.srsoftware.tools.Strings.uuid;
import static de.srsoftware.tools.result.Error.error;
import static java.lang.System.Logger.Level.WARNING;

import de.srsoftware.oidc.api.*;
import de.srsoftware.oidc.api.data.AccessToken;
import de.srsoftware.oidc.api.data.User;
import de.srsoftware.tools.PasswordHasher;
import de.srsoftware.tools.result.Error;
import de.srsoftware.tools.result.Payload;
import de.srsoftware.tools.result.Result;
import java.io.File;
import java.util.*;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

public class EncryptedUserServiceTest extends UserServiceTest {
	private static final System.Logger	    LOG = System.getLogger(EncryptedUserServiceTest.class.getSimpleName());
	private static class InMemoryUserService implements UserService {
		private final PasswordHasher<String> hasher;
		private final HashMap<String, User> users = new HashMap<>();

		public InMemoryUserService(PasswordHasher<String> hasher) {
			this.hasher = hasher;
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
			users.remove(user.uuid());
			return this;
		}

		@Override
		public Optional<User> forToken(String accessToken) {
			return Optional.empty();
		}

		@Override
		public UserService init(User defaultUser) {
			if (users.isEmpty()) users.put(defaultUser.uuid(), defaultUser);
			return this;
		}

		@Override
		public List<User> list() {
			return List.copyOf(users.values());
		}

		@Override
		public Set<User> find(String idOrEmail) {
			return list().stream().filter(user -> user.uuid().equals(idOrEmail) || user.email().equals(idOrEmail)).collect(Collectors.toSet());
		}

		@Override
		public Optional<User> load(String id) {
			return nullable(users.get(id));
		}

		@Override
		public Result<User> login(String username, String password) {
			var optLock = getLock(username);
			if (optLock.isPresent()) {
				var lock = optLock.get();
				LOG.log(WARNING, "{} is locked after {} failed logins. Lock will be released at {}", username, lock.attempts(), lock.releaseTime());
				Error<User> err = error(ERROR_LOCKED);
				return err.addData(ATTEMPTS, lock.attempts(), RELEASE, lock.releaseTime());
			}

			for (var entry : users.entrySet()) {
				var user = entry.getValue();
				if (user.username().equals(username) && passwordMatches(password, user)) {
					unlock(username);
					return Payload.of(user);
				}
			}
			var lock = lock(username);
			LOG.log(WARNING, "Login failed for {0} → locking account until {1}", username, lock.releaseTime());
			Error<User> err = error(ERROR_LOGIN_FAILED);
			return err.addData(RELEASE, lock.releaseTime());
		}

		@Override
		public boolean passwordMatches(String plaintextPassword, User user) {
			return hasher.matches(plaintextPassword, user.hashedPassword());
		}

		@Override
		public UserService save(User user) {
			users.put(user.uuid(), user);
			return this;
		}

		@Override
		public UserService updatePassword(User user, String plaintextPassword) {
			var old = users.get(user.uuid());
			save(user.hashedPassword(hasher.hash(plaintextPassword, uuid())));
			return this;
		}
	}
	private final File  storage = new File("/tmp/" + UUID.randomUUID());
	private UserService userService;

	@AfterEach
	public void tearDown() {
		if (storage.exists()) {
			var ignored = storage.delete();
		}
	}

	@BeforeEach
	public void setup() {
		tearDown();
		String	    key     = uuid();
		String	    salt    = uuid();
		InMemoryUserService backend = new InMemoryUserService(hasher());
		userService	            = new EncryptedUserService(backend, key, salt, hasher());
	}

	@Override
	protected UserService userService() {
		return userService;
	}
}
