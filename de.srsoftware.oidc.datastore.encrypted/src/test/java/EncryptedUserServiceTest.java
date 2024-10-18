/* © SRSoftware 2024 */
import static de.srsoftware.utils.Optionals.nullable;
import static de.srsoftware.utils.Strings.uuid;
import static java.lang.System.Logger.Level.WARNING;

import de.srsoftware.oidc.api.Result;
import de.srsoftware.oidc.api.UserService;
import de.srsoftware.oidc.api.UserServiceTest;
import de.srsoftware.oidc.api.data.AccessToken;
import de.srsoftware.oidc.api.data.User;
import de.srsoftware.oidc.datastore.encrypted.EncryptedUserService;
import de.srsoftware.utils.PasswordHasher;
import java.io.File;
import java.util.*;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

public class EncryptedUserServiceTest extends UserServiceTest {
	private static final System.Logger           LOG = System.getLogger(EncryptedUserServiceTest.class.getSimpleName());
	private class InMemoryUserService implements UserService {
		private final PasswordHasher<String> hasher;
		private HashMap<String, User>	     users = new HashMap<>();

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
				return Optional.empty();
			}

			for (var entry : users.entrySet()) {
				var user = entry.getValue();
				if (user.username().equals(username) && passwordMatches(password, user)) {
					unlock(username);
					return Optional.of(user);
				}
			}
			lock(username);
			return Optional.empty();
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
	private File	    storage = new File("/tmp/" + UUID.randomUUID());
	private UserService userService;
	private String	    key, salt;

	@AfterEach
	public void tearDown() {
		if (storage.exists()) storage.delete();
	}

	@BeforeEach
	public void setup() {
		tearDown();
		key	            = uuid();
		salt	            = uuid();
		InMemoryUserService backend = new InMemoryUserService(hasher());
		userService	            = new EncryptedUserService(backend, key, salt, hasher());
	}

	@Override
	protected UserService userService() {
		return userService;
	}
}
