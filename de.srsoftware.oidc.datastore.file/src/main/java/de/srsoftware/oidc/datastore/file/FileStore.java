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

public class FileStore implements AuthorizationService, ClientService, SessionService, UserService {
	private static final String AUTHORIZATIONS = "authorizations";
	private static final String CLIENTS        = "clients";
	private static final String CODES          = "codes";
	private static final String EXPIRATION     = "expiration";
	private static final String NAME           = "name";
	private static final String REDIRECT_URIS  = "redirect_uris";
	private static final String SECRET         = "secret";
	private static final String SESSIONS       = "sessions";
	private static final String USERS          = "users";
	private static final String USER           = "user";

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
		if (!json.has(AUTHORIZATIONS)) json.put(AUTHORIZATIONS, new JSONObject());
		if (!json.has(CLIENTS)) json.put(CLIENTS, new JSONObject());
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
			var users    = json.getJSONObject(USERS);
			var userData = users.getJSONObject(userId);
			return userOf(userData, userId);
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
					return userOf(userData, userId);
				}
			}
			return Optional.empty();
		} catch (Exception e) {
			return Optional.empty();
		}
	}

	@Override
	public boolean passwordMatches(String password, String hashedPassword) {
		return passwordHasher.matches(password, hashedPassword);
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
		var salt	      = passwordHasher.salt(oldHashedPassword);
		user.hashedPassword(passwordHasher.hash(plaintextPassword, salt));
		return save(user);
	}

	private Optional<User> userOf(JSONObject json, String userId) {
		var user  = new User(json.getString(USERNAME), json.getString(PASSWORD), json.getString(REALNAME), json.getString(EMAIL), userId);
		var perms = json.getJSONArray(PERMISSIONS);
		for (Object perm : perms) {
			try {
				if (perm instanceof String s) perm = Permission.valueOf(s);
				if (perm instanceof Permission p) user.add(p);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		return Optional.of(user);
	}


	/*** Session Service Methods ***/

	// TODO: prolong session on user activity
	// TODO: drop expired sessions

	@Override
	public Session createSession(User user) {
		var now	 = Instant.now();
		var endOfSession = now.plus(sessionDuration);
		return save(new Session(user, endOfSession, java.util.UUID.randomUUID().toString()));
	}

	@Override
	public SessionService dropSession(String sessionId) {
		json.getJSONObject(SESSIONS).remove(sessionId);
		save();
		return this;
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
			dropSession(sessionId);
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
	public Optional<Client> getClient(String clientId) {
		var clients = json.getJSONObject(CLIENTS);
		if (clients.has(clientId)) return Optional.of(toClient(clientId, clients.getJSONObject(clientId)));
		return Optional.empty();
	}


	@Override
	public List<Client> listClients() {
		var clients = json.getJSONObject(CLIENTS);
		var list    = new ArrayList<Client>();
		for (var clientId : clients.keySet()) list.add(toClient(clientId, clients.getJSONObject(clientId)));
		return list;
	}

	@Override
	public FileStore remove(Client client) {
		var clients = json.getJSONObject(CLIENTS);
		if (clients.has(client.id())) clients.remove(client.id());
		return save();
	}

	@Override
	public ClientService save(Client client) {
		json.getJSONObject(CLIENTS).put(client.id(), Map.of(NAME, client.name(), SECRET, client.secret(), REDIRECT_URIS, client.redirectUris()));
		save();
		return this;
	}

	private Client toClient(String clientId, JSONObject clientData) {
		var redirectUris = new HashSet<String>();
		for (var o : clientData.getJSONArray(REDIRECT_URIS)) {
			if (o instanceof String s) redirectUris.add(s);
		}
		return new Client(clientId, clientData.getString(NAME), clientData.getString(SECRET), redirectUris);
	}


	/*** Authorization service methods ***/

	@Override
	public Optional<Authorization> forCode(String code) {
		var authorizations = json.getJSONObject(AUTHORIZATIONS);
		if (!authorizations.has(code)) return Optional.empty();
		String authId = authorizations.getString(code);
		if (!authorizations.has(authId)) {
			authorizations.remove(code);
			return Optional.empty();
		}
		try {
			var expiration = Instant.ofEpochSecond(authorizations.getLong(authId));
			if (expiration.isAfter(Instant.now())) {
				String[] parts = authId.split("@");
				return Optional.of(new Authorization(parts[1], parts[0], expiration));
			}
			authorizations.remove(authId);
		} catch (Exception ignored) {
		}

		return Optional.empty();
	}

	@Override
	public AuthorizationService addCode(Client client, User user, String code) {
		var authorizations = json.getJSONObject(AUTHORIZATIONS);
		authorizations.put(code, authorizationId(user, client));
		save();
		return this;
	}

	@Override
	public AuthorizationService authorize(Client client, User user, Instant expiration) {
		var authorizations = json.getJSONObject(AUTHORIZATIONS);
		authorizations.put(authorizationId(user, client), expiration.getEpochSecond());
		return this;
	}

	private String authorizationId(User user, Client client) {
		return String.join("@", user.uuid(), client.id());
	}

	@Override
	public boolean isAuthorized(Client client, User user) {
		var authorizations = json.getJSONObject(AUTHORIZATIONS);
		var authId	   = authorizationId(user, client);
		if (!authorizations.has(authId)) return false;

		try {
			if (Instant.ofEpochSecond(authorizations.getLong(authId)).isAfter(Instant.now())) return true;
		} catch (Exception ignored) {
		}
		revoke(client, user);
		return false;
	}

	@Override
	public List<User> authorizedUsers(Client client) {
		return List.of();
	}

	@Override
	public List<Client> authorizedClients(User user) {
		return List.of();
	}

	@Override
	public AuthorizationService revoke(Client client, User user) {
		var authorizations = json.getJSONObject(AUTHORIZATIONS);
		var authId	   = authorizationId(user, client);
		if (!authorizations.has(authId)) return this;
		authorizations.remove(authId);
		return save();
	}
}
