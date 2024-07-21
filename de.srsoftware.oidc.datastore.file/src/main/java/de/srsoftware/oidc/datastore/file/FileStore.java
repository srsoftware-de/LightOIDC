/* © SRSoftware 2024 */
package de.srsoftware.oidc.datastore.file; /* © SRSoftware 2024 */
import static de.srsoftware.oidc.api.User.*;

import de.srsoftware.oidc.api.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import org.json.JSONObject;

public class FileStore implements ClientService, SessionService, UserService {
	private static final String EXPIRATION = "expiration";
	private static final String SESSIONS   = "sessions";
	private static final String USERS      = "users";
	private static final String USER       = "user";

	private final Path       storageFile;
	private final JSONObject json;
	private final PasswordHasher<String> passwordHasher;
	private Duration	     sessionDuration = Duration.of(10, ChronoUnit.MINUTES);

	public FileStore(File storage, PasswordHasher<String> passwordHasher) throws IOException {
		this.storageFile    = storage.toPath();
		this.passwordHasher = passwordHasher;

		if (!storage.exists()) {
			var parent = storage.getParentFile();
			if (!parent.exists() && !parent.mkdirs()) throw new FileNotFoundException("Failed to create directory %s".formatted(parent));
			Files.writeString(storageFile, "{}");
		}
		json = new JSONObject(Files.readString(storageFile));
	}

	private FileStore save() {
		try {
			Files.writeString(storageFile, json.toString(2));
			return this;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/*** User Service Methods ***/


	@Override
	public UserService delete(User user) {
		return null;
	}


	@Override
	public FileStore init(User defaultUser) {
		if (!json.has(SESSIONS)) json.put(SESSIONS, new JSONObject());
		if (!json.has(USERS)) save(defaultUser);
		return this;
	}



	@Override
	public List<User> list() {
		return List.of();
	}


	@Override
	public Optional<User> load(String userId) {
		try {
			var users = json.getJSONObject(USERS);
			var userData = users.getJSONObject(userId);
			return userOf(userData,userId);
		} catch (Exception ignored) {
		}
		return Optional.empty();
	}

	@Override
	public Optional<User> load(String username, String password) {
		try {
			var users = json.getJSONObject(USERS);
			var uuids = users.keySet();
			for (String userId : uuids) {
				var userData = users.getJSONObject(userId);
				if (!userData.getString(USERNAME).equals(username)) continue;
				var hashedPass = userData.getString(PASSWORD);
				if (passwordHasher.matches(password, hashedPass)) {
					return userOf(userData,userId);
				}
			}
			return Optional.empty();
		} catch (Exception e) {
			return Optional.empty();
		}
	}

	@Override
	public boolean passwordMatches(String password, String hashedPassword) {
		return passwordHasher.matches(password,hashedPassword);
	}

	@Override
	public FileStore save(User user) {
		JSONObject users;
		if (!json.has(USERS)) {
			json.put(USERS, users = new JSONObject());
		} else {
			users = json.getJSONObject(USERS);
		}
		users.put(user.uuid(), user.map(true));
		return save();
	}

	@Override
	public FileStore updatePassword(User user, String plaintextPassword) {
		var oldHashedPassword = user.hashedPassword();
		var salt = passwordHasher.salt(oldHashedPassword);
		user.hashedPassword(passwordHasher.hash(plaintextPassword,salt));
		return save(user);
	}

	private Optional<User> userOf(JSONObject json, String userId){
		var user = new User(json.getString(USERNAME), json.getString(PASSWORD), json.getString(REALNAME), json.getString(EMAIL), userId);
		var perms = json.getJSONArray(PERMISSIONS);
		for (Object perm : perms){
			try {
				if (perm instanceof String s) perm = Permission.valueOf(s);
				if (perm instanceof Permission p) user.add(p);
			} catch (Exception e){
				e.printStackTrace();
			}
		}

		return Optional.of(user);
	}


	/*** Session Service Methods ***/

	@Override
	public Session createSession(User user) {
		var now	 = Instant.now();
		var endOfSession = now.plus(sessionDuration);
		return save(new Session(user, endOfSession, java.util.UUID.randomUUID().toString()));
	}

	@Override
	public SessionService dropSession(String sessionId) {
		return null;
	}

	@Override
	public Session extend(String sessionId) {
		return null;
	}

	@Override
	public Optional<Session> retrieve(String sessionId) {
		var sessions = json.getJSONObject(SESSIONS);
		try {
			var session    = sessions.getJSONObject(sessionId);
			var userId     = session.getString(USER);
			var expiration = Instant.ofEpochSecond(session.getLong(EXPIRATION));
			if (expiration.isAfter(Instant.now())) {
				return load(userId).map(user -> new Session(user, expiration, sessionId));
			}
		} catch (Exception ignored) {
		}
		return Optional.empty();
	}

	private Session save(Session session) {
		json.getJSONObject(SESSIONS).put(session.id(), Map.of(USER, session.user().uuid(), EXPIRATION, session.expiration().getEpochSecond()));
		save();
		return session;
	}

	@Override
	public SessionService setDuration(Duration duration) {
		return null;
	}

	/** client service methods **/

	@Override
	public ClientService add(Client client) {
		return null;
	}

	@Override
	public Optional<Client> getClient(String clientId) {
		return Optional.empty();
	}

	@Override
	public List<Client> listClients() {
		return List.of();
	}

	@Override
	public ClientService remove(Client client) {
		return null;
	}

	@Override
	public ClientService update(Client client) {
		return null;
	}
}
