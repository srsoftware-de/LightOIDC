/* © SRSoftware 2025 */
package de.srsoftware.oidc.datastore.encrypted;

import static de.srsoftware.oidc.api.Constants.*;
import static de.srsoftware.tools.result.Error.error;
import static java.lang.System.Logger.Level.WARNING;
import static java.util.Optional.empty;

import de.srsoftware.oidc.api.UserService;
import de.srsoftware.oidc.api.data.AccessToken;
import de.srsoftware.oidc.api.data.User;
import de.srsoftware.tools.PasswordHasher;
import de.srsoftware.tools.result.*;
import de.srsoftware.tools.result.Error;
import de.srsoftware.tools.result.Payload;
import de.srsoftware.tools.result.Result;
import java.util.*;

public class EncryptedUserService extends EncryptedConfig implements UserService {
	private static final System.Logger LOG = System.getLogger(EncryptedUserService.class.getSimpleName());
	private final UserService	   backend;
	private final PasswordHasher	   hasher;

	public EncryptedUserService(UserService backend, String key, String salt, PasswordHasher passHasher) {
		super(key, salt);
		this.backend = backend;
		hasher       = passHasher;
	}

	@Override
	public AccessToken accessToken(User user) {
		return backend.accessToken(encrypt(user));
	}

	@Override
	public Optional<User> consumeToken(String accessToken) {
		return backend.consumeToken(accessToken).map(this::decrypt);
	}

	public User decrypt(User secret) {
		var decrypted = new User(decrypt(secret.username()), decrypt(secret.hashedPassword()), decrypt(secret.realName()), decrypt(secret.email()), decrypt(secret.uuid())).sessionDuration(secret.sessionDuration());
		secret.permissions().forEach(decrypted::add);
		return decrypted;
	}

	@Override
	public UserService delete(User user) {
		for (var encryptedUser : backend.list()) {
			var decryptedUser = decrypt(encryptedUser);
			if (decryptedUser.uuid().equals(user.uuid())) {
				backend.delete(encryptedUser);
				break;
			}
		}
		return this;
	}

	public User encrypt(User plain) {
		var encrypted = new User(encrypt(plain.username()), encrypt(plain.hashedPassword()), encrypt(plain.realName()), encrypt(plain.email()), encrypt(plain.uuid())).sessionDuration(plain.sessionDuration());
		plain.permissions().forEach(encrypted::add);
		return encrypted;
	}

	@Override
	public Optional<User> forToken(String accessToken) {
		return backend.forToken(accessToken).map(this::decrypt);
	}

	@Override
	public UserService init(User defaultUser) {
		backend.init(encrypt(defaultUser));
		return this;
	}

	@Override
	public List<User> list() {
		return backend.list().stream().map(this::decrypt).toList();
	}

	@Override
	public Set<User> find(String idOrEmail) {
		if (idOrEmail == null || idOrEmail.isBlank()) return Set.of();
		var matching = new HashMap<String, User>();
		for (var encryptedUser : backend.list()) {
			var decryptedUser = decrypt(encryptedUser);
			if (idOrEmail.equals(decryptedUser.uuid()) || idOrEmail.equals(decryptedUser.email()) || idOrEmail.equals(decryptedUser.username()) || decryptedUser.realName().contains(idOrEmail)) matching.put(decryptedUser.uuid(), decryptedUser);
		}
		return Set.copyOf(matching.values());
	}

	@Override
	public Optional<User> load(String id) {
		if (id == null || id.isBlank()) return empty();
		for (var encryptedUser : backend.list()) {
			var decryptedUser = decrypt(encryptedUser);
			if (id.equals(decryptedUser.uuid())) return Optional.of(decryptedUser);
		}
		return empty();
	}

	@Override
	public Result<User> login(String username, String password) {
		if (username == null || username.isBlank()) return error(ERROR_NO_USERNAME);
		var optLock = getLock(username);
		if (optLock.isPresent()) {
			var lock = optLock.get();
			LOG.log(WARNING, "{0} is locked after {1} failed logins. Lock will be released at {2}", username, lock.attempts(), lock.releaseTime());
			Error<User> err = error(ERROR_LOCKED);
			return err.addData(ATTEMPTS, lock.attempts(), RELEASE, lock.releaseTime());
		}
		for (var encryptedUser : backend.list()) {
			var decryptedUser = decrypt(encryptedUser);
			var match	  = List.of(decryptedUser.username(), decryptedUser.realName(), decryptedUser.email()).contains(username);
			if (match && hasher.matches(password, decryptedUser.hashedPassword())) {
				this.unlock(username);
				return Payload.of(decryptedUser);
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
		delete(user);
		backend.save(encrypt(user));
		return this;
	}

	@Override
	public UserService updatePassword(User user, String plaintextPassword) {
		var pass = hasher.hash(plaintextPassword, user.uuid());
		return save(user.hashedPassword(pass));
	}
}
