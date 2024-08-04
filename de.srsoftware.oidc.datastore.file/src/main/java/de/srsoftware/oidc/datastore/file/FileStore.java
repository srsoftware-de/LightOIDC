/* © SRSoftware 2024 */
package de.srsoftware.oidc.datastore.file; /* © SRSoftware 2024 */
import static de.srsoftware.oidc.api.Constants.EXPIRATION;
import static de.srsoftware.oidc.api.User.*;
import static de.srsoftware.utils.Optionals.nullable;
import static de.srsoftware.utils.Strings.uuid;
import static java.lang.System.Logger.Level.WARNING;
import static java.util.Optional.empty;

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

public class FileStore implements ClaimAuthorizationService, ClientService, SessionService, UserService {
	private static final String AUTHORIZATIONS	 = "authorizations";
	private static final String CLIENTS	 = "clients";
	private static final String CODES	 = "codes";
	private static final System.Logger LOG	 = System.getLogger(FileStore.class.getSimpleName());
	private static final String	   NAME	 = "name";
	private static final String	   REDIRECT_URIS = "redirect_uris";
	private static final String	   SECRET	 = "secret";
	private static final String	   SESSIONS	 = "sessions";
	private static final String	   USERS	 = "users";
	private static final String	   USER	 = "user";

	private final Path       storageFile;
	private final JSONObject json;
	private final PasswordHasher<String> passwordHasher;
	private Duration	     sessionDuration = Duration.of(10, ChronoUnit.MINUTES);
	private Map<String, Client>	     clients	     = new HashMap<>();
	private Map<String, User>	     accessTokens    = new HashMap<>();
	private Map<String, Authorization>   authCodes	     = new HashMap<>();

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
	public String accessToken(User user) {
		var token = uuid();
		accessTokens.put(token, Objects.requireNonNull(user));
		return token;
	}


	@Override
	public UserService delete(User user) {
		return null;
	}

	@Override
	public Optional<User> forToken(String accessToken) {
		return nullable(accessTokens.get(accessToken));
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
		return empty();
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
			return empty();
		} catch (Exception e) {
			return empty();
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
		return save(new Session(user, endOfSession, uuid().toString()));
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
		return empty();
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
		var client = clients.get(clientId);
		if (client != null) return Optional.of(client);
		var clientsJson = json.getJSONObject(CLIENTS);
		if (clientsJson.has(clientId)) {
			client = toClient(clientId, clientsJson.getJSONObject(clientId));
			clients.put(clientId, client);
			return Optional.of(client);
		}
		return empty();
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

	/*** ClaimAuthorizationService methods ***/
	private String authCode(User user, Client client, Authorization authorization) {
		var code = uuid();
		authCodes.put(code, authorization);
		return code;
	}

	@Override
	public ClaimAuthorizationService authorize(User user, Client client, Collection<String> scopes, Instant expiration) {
		LOG.log(WARNING, "{0}.authorize({1}, {2}, {3}, {4}) not implemented", getClass().getSimpleName(), user.realName(), client.name(), scopes, expiration);
		var authorizations = json.getJSONObject(AUTHORIZATIONS);
		if (!authorizations.has(user.uuid())) authorizations.put(user.uuid(), new JSONObject());
		var userAuthorizations = authorizations.getJSONObject(user.uuid());
		if (!userAuthorizations.has(client.id())) userAuthorizations.put(client.id(), new JSONObject());
		var clientScopes = userAuthorizations.getJSONObject(client.id());
		for (var scope : scopes) clientScopes.put(scope, expiration.getEpochSecond());
		save();
		return this;
	}


	@Override
	public Optional<Authorization> consumeAuthorization(String authCode) {
		return nullable(authCodes.remove(authCode));
	}

	@Override
	public AuthResult getAuthorization(User user, Client client, Collection<String> scopes) {
		var authorizations     = json.getJSONObject(AUTHORIZATIONS);
		var userAuthorizations = authorizations.has(user.uuid()) ? authorizations.getJSONObject(user.uuid()) : null;
		if (userAuthorizations == null) return unauthorized(scopes);
		var clientScopes = userAuthorizations.has(client.id()) ? userAuthorizations.getJSONObject(client.id()) : null;
		if (clientScopes == null) return unauthorized(scopes);
		var     now	           = Instant.now();
		var     authorizedScopes   = new HashSet<String>();
		var     unauthorizedScopes = new HashSet<String>();
		Instant earliestExpiration = null;
		for (var scope : scopes) {
			if (clientScopes.has(scope)) {
				var expiration = Instant.ofEpochSecond(clientScopes.getLong(scope));
				if (expiration.isAfter(now)) {
					authorizedScopes.add(scope);
					if (earliestExpiration == null || expiration.isBefore(earliestExpiration)) earliestExpiration = expiration;
				} else {
					unauthorizedScopes.add(scope);
				}
			}
		}

		var authorization = new Authorization(client.id(), user.uuid(), new AuthorizedScopes(authorizedScopes, earliestExpiration));
		return new AuthResult(authorization.scopes(), unauthorizedScopes, authCode(user, client, authorization));
	}

	private AuthResult unauthorized(Collection<String> scopes) {
		return new AuthResult(null, new HashSet<>(scopes), null);
	}
}
