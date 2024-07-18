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

public class FileStore implements SessionService, UserService {
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
			var user  = users.getJSONObject(userId);
			return Optional.of(new User(user.getString(USERNAME), user.getString(PASSWORD), user.getString(REALNAME), user.getString(EMAIL), userId));
		} catch (Exception ignored) {
		}
		return Optional.empty();
	}

	@Override
	public Optional<User> load(String username, String password) {
		try {
			var users = json.getJSONObject(USERS);
			var uuids = users.keySet();
			for (String uuid : uuids) {
				var user = users.getJSONObject(uuid);
				if (!user.getString(USERNAME).equals(username)) continue;
				var hashedPass = user.getString(PASSWORD);
				if (passwordHasher.matches(password, hashedPass)) {
					return Optional.of(new User(username, hashedPass, user.getString(REALNAME), user.getString(EMAIL), uuid));
				}
			}
			return Optional.empty();
		} catch (Exception e) {
			return Optional.empty();
		}
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

	/*** Session Service Methods ***/

	@Override
	public Session createSession(User user) {
		var now	 = Instant.now();
		var endOfSession = now.plus(sessionDuration);
		return save(new Session(user, endOfSession, UUID.randomUUID().toString()));
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
}
