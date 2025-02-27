/* © SRSoftware 2024 */
package de.srsoftware.oidc.datastore.file; /* © SRSoftware 2024 */
import static de.srsoftware.oidc.api.Constants.*;
import static de.srsoftware.oidc.api.data.User.*;
import static de.srsoftware.utils.Optionals.nullable;
import static de.srsoftware.utils.Strings.uuid;
import static java.lang.System.Logger.Level.*;
import static java.time.temporal.ChronoUnit.SECONDS;
import static java.util.Optional.empty;

import de.srsoftware.oidc.api.*;
import de.srsoftware.oidc.api.data.*;
import de.srsoftware.utils.Error;
import de.srsoftware.utils.PasswordHasher;
import de.srsoftware.utils.Payload;
import de.srsoftware.utils.Result;
import jakarta.mail.Authenticator;
import jakarta.mail.PasswordAuthentication;
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

public class FileStore implements AuthorizationService, ClientService, SessionService, UserService, MailConfig {
	private static final System.Logger LOG	  = System.getLogger(FileStore.class.getSimpleName());
	private static final String	   AUTHORIZATIONS = "authorizations";
	private static final String	   CLIENTS	  = "clients";
	private static final String	   REDIRECT_URIS  = "redirect_uris";
	private static final String	   SESSIONS	  = "sessions";
	private static final String	   USERS	  = "users";
	private static final List<String> KEYS	  = List.of(USERNAME, EMAIL, REALNAME);

	private final Path       storageFile;
	private final JSONObject json;
	private final PasswordHasher<String> passwordHasher;
	private Map<String, AccessToken>     accessTokens = new HashMap<>();
	private Map<String, Authorization>   authCodes	  = new HashMap<>();
	private Map<String, String>	     nonceMap	  = new HashMap<>();
	private Authenticator	     auth;

	public FileStore(File storage, PasswordHasher<String> passwordHasher) throws IOException {
		this.storageFile    = storage.toPath();
		this.passwordHasher = passwordHasher;

		if (!storage.exists()) {
			var parent = storage.getParentFile();
			if (!parent.exists() && !parent.mkdirs()) throw new FileNotFoundException("Failed to create directory %s".formatted(parent));
			Files.writeString(storageFile, "{}");
		}
		json = new JSONObject(Files.readString(storageFile));
		auth = null;  // lazy init!
	}

	private void cleanUp() {
		var now      = Instant.now();
		var sessions = sessions();
		LOG.log(DEBUG, "cleaning up sessions…");
		var sessionIds = Set.copyOf(sessions.keySet());
		for (var sessionId : sessionIds) {
			var session    = sessions.getJSONObject(sessionId);
			var expiration = Instant.ofEpochSecond(session.getLong(EXPIRATION));
			if (expiration.isBefore(now)) {
				sessions.remove(sessionId);
				LOG.log(DEBUG, "removed old session {0}.", sessionId);
			}
		}
		if (json.has(AUTHORIZATIONS)) {
			var authorizations     = json.getJSONObject(AUTHORIZATIONS);
			var authorizationUsers = Set.copyOf(authorizations.keySet());
			for (var userId : authorizationUsers) {
				var clients   = authorizations.getJSONObject(userId);
				var clientIds = Set.copyOf(clients.keySet());
				for (var clientId : clientIds) {
					var client = clients.getJSONObject(clientId);
					var scopes = Set.copyOf(client.keySet());
					for (var scope : scopes) {
						var expiration = Instant.ofEpochSecond(client.getLong(scope));
						if (expiration.isBefore(now)) {
							client.remove(scope);
						}
					}
					// if (client.isEmpty()) clients.remove(clientId); // keep client as mark for ClientController.dash
				}
				if (clients.isEmpty()) authorizations.remove(userId);
			}
		}
	}


	public FileStore save() {
		cleanUp();
		try {
			Files.writeString(storageFile, json.toString(2));
			return this;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/*** User Service Methods ***/

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

	@Override
	public UserService delete(User user) {
		if (!json.has(USERS)) return this;
		var users = json.getJSONObject(USERS);
		users.remove(user.uuid());
		return save();
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
	public FileStore init(User defaultUser) {
		if (!json.has(USERS)) json.put(USERS, new JSONObject());
		var users = json.getJSONObject(USERS);
		if (users.length() < 1) save(defaultUser);
		return this;
	}


	@Override
	public Set<User> find(String key) {
		if (!json.has(USERS)) return Set.of();
		var users  = json.getJSONObject(USERS);
		var result = new HashSet<User>();
		for (var id : users.keySet()) {
			var data = users.getJSONObject(id);
			if (KEYS.stream().map(data::getString).anyMatch(val -> val.equals(key))) User.of(data, id).ifPresent(result::add);
		}
		return result;
	}

	@Override
	public List<User> list() {
		List<User> result = new ArrayList<>();
		if (!json.has(USERS)) return result;
		var users = json.getJSONObject(USERS);
		for (var uid : users.keySet()) User.of(users.getJSONObject(uid), uid).ifPresent(result::add);
		return result;
	}


	@Override
	public Optional<User> load(String userId) {
		if (!json.has(USERS)) return empty();
		try {
			var users    = json.getJSONObject(USERS);
			var userData = users.getJSONObject(userId);
			return User.of(userData, userId);
		} catch (Exception ignored) {
		}
		return empty();
	}

	@Override
	public Result<User> login(String username, String password) {
		if (!json.has(USERS)) return Error.message(ERROR_LOGIN_FAILED);
		if (username == null || username.isBlank()) return Error.message(ERROR_NO_USERNAME);
		var optLock = getLock(username);
		if (optLock.isPresent()) {
			var lock = optLock.get();
			LOG.log(WARNING, "{0} is locked after {1} failed logins. Lock will be released at {2}", username, lock.attempts(), lock.releaseTime());
			return Error.message(ERROR_LOCKED, ATTEMPTS, lock.attempts(), RELEASE, lock.releaseTime());
		}
		try {
			var users = json.getJSONObject(USERS);
			for (String userId : users.keySet()) {
				var userData = users.getJSONObject(userId);

				if (KEYS.stream().map(userData::getString).noneMatch(val -> val.equals(username))) continue;
				var loadedUser = User.of(userData, userId).filter(u -> passwordMatches(password, u));
				if (loadedUser.isPresent()) {
					unlock(username);
					return Payload.of(loadedUser.get());
				}
			}
			var lock = lock(username);
			LOG.log(WARNING, "Login failed for {0} → locking account until {1}", username, lock.releaseTime());
			return Error.message(ERROR_LOGIN_FAILED, RELEASE, lock.releaseTime());
		} catch (Exception e) {
			return Error.message(ERROR_LOGIN_FAILED);
		}
	}

	@Override
	public boolean passwordMatches(String password, User user) {
		return passwordHasher.matches(password, user.hashedPassword());
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
		return save(user.hashedPassword(passwordHasher.hash(plaintextPassword, uuid())));
	}

	/*** Session Service Methods ***/

	// TODO: prolong session on user activity
	// TODO: drop expired sessions

	@Override
	public Session createSession(User user, boolean trustBrowser) {
		var now	 = Instant.now();
		var endOfSession = now.plus(user.sessionDuration()).truncatedTo(SECONDS);
		return save(new Session(user.uuid(), endOfSession, uuid(), trustBrowser));
	}

	@Override
	public SessionService dropSession(String sessionId) {
		sessions().remove(sessionId);
		save();
		return this;
	}

	@Override
	public Session extend(Session session, User user) {
		var endOfSession = Instant.now().plus(user.sessionDuration());
		return save(new Session(user.uuid(), endOfSession, session.id(), session.trustBrowser()));
	}

	private JSONObject sessions() {
		if (!json.has(SESSIONS)) json.put(SESSIONS, new JSONObject());
		return json.getJSONObject(SESSIONS);
	}

	@Override
	public Optional<Session> retrieve(String sessionId) {
		try {
			var session	 = sessions().getJSONObject(sessionId);
			var userId	 = session.getString(USER);
			var expiration	 = Instant.ofEpochSecond(session.getLong(EXPIRATION)).truncatedTo(SECONDS);
			var trustBrowser = session.getBoolean(TRUST);
			if (expiration.isAfter(Instant.now())) return Optional.of(new Session(userId, expiration, sessionId, trustBrowser));
			dropSession(sessionId);
		} catch (Exception ignored) {
		}
		return empty();
	}

	private Session save(Session session) {
		sessions().put(session.id(), Map.of(USER, session.userId(), EXPIRATION, session.expiration().getEpochSecond(), TRUST, session.trustBrowser()));
		save();
		return session;
	}

	/** client service methods **/

	@Override
	public Optional<Client> getClient(String clientId) {
		if (!json.has(CLIENTS)) return empty();
		var clientsJson = json.getJSONObject(CLIENTS);
		if (clientsJson.has(clientId)) {
			var client = toClient(clientId, clientsJson.getJSONObject(clientId));
			return Optional.of(client);
		}
		return empty();
	}


	@Override
	public List<Client> listClients() {
		if (!json.has(CLIENTS)) return List.of();
		var clients = json.getJSONObject(CLIENTS);
		var list    = new ArrayList<Client>();
		for (var clientId : clients.keySet()) list.add(toClient(clientId, clients.getJSONObject(clientId)));
		return list;
	}

	@Override
	public FileStore remove(String clientId) {
		if (!json.has(CLIENTS)) return this;
		var clients = json.getJSONObject(CLIENTS);
		if (clients.has(clientId)) clients.remove(clientId);
		return save();
	}

	@Override
	public ClientService save(Client client) {
		if (!json.has(CLIENTS)) json.put(CLIENTS, new JSONObject());
		json.getJSONObject(CLIENTS).put(client.id(), client.map());
		save();
		return this;
	}

	private Client toClient(String clientId, JSONObject clientData) {
		var redirectUris = new HashSet<String>();
		if (clientData.has(REDIRECT_URIS))
			for (var o : clientData.getJSONArray(REDIRECT_URIS)) {
				if (o instanceof String s) redirectUris.add(s);
			}
		var client = new Client(clientId, clientData.getString(NAME), clientData.getString(SECRET), redirectUris);
		if (clientData.has(LANDING_PAGE)) client.landingPage(clientData.getString(LANDING_PAGE));
		if (clientData.has(TOKEN_VALIDITY)) client.tokenValidity(Duration.ofMinutes(clientData.getLong(TOKEN_VALIDITY)));
		return client;
	}

	/*** AuthorizationService methods ***/
	private String authCode(Authorization authorization) {
		var code = uuid();
		authCodes.put(code, authorization);
		return code;
	}

	@Override
	public AuthorizationService authorize(String userId, String clientId, Collection<String> scopes, Instant expiration) {
		if (!json.has(AUTHORIZATIONS)) json.put(AUTHORIZATIONS, new JSONObject());
		var authorizations = json.getJSONObject(AUTHORIZATIONS);
		if (!authorizations.has(userId)) authorizations.put(userId, new JSONObject());
		var userAuthorizations = authorizations.getJSONObject(userId);
		if (!userAuthorizations.has(clientId)) userAuthorizations.put(clientId, new JSONObject());
		var clientScopes = userAuthorizations.getJSONObject(clientId);
		for (var scope : scopes) clientScopes.put(scope, expiration.getEpochSecond());
		save();
		return this;
	}

	@Override
	public List<String> authorizedClients(String userId) {
		if (!json.has(AUTHORIZATIONS)) return List.of();
		var authorizations = json.getJSONObject(AUTHORIZATIONS);
		if (!authorizations.has(userId)) return List.of();
		var clients = authorizations.getJSONObject(userId);
		return new ArrayList<>(clients.keySet());
	}


	@Override
	public Optional<Authorization> consumeAuthorization(String authCode) {
		return nullable(authCodes.remove(authCode));
	}

	@Override
	public Optional<String> consumeNonce(String userId, String clientId) {
		var nonceKey = String.join("@", userId, clientId);
		return nullable(nonceMap.get(nonceKey));
	}

	@Override
	public AuthResult getAuthorization(String userId, String clientId, Collection<String> scopes) {
		if (!json.has(AUTHORIZATIONS)) return unauthorized(scopes);
		var authorizations     = json.getJSONObject(AUTHORIZATIONS);
		var userAuthorizations = authorizations.has(userId) ? authorizations.getJSONObject(userId) : null;
		if (userAuthorizations == null) return unauthorized(scopes);
		var clientScopes = userAuthorizations.has(clientId) ? userAuthorizations.getJSONObject(clientId) : null;
		if (clientScopes == null) return unauthorized(scopes);
		var     now	           = Instant.now();
		var     authorizedScopes   = new HashSet<String>();
		var     unauthorizedScopes = new HashSet<String>();
		Instant earliestExpiration = null;
		for (var scope : scopes) {
			if (clientScopes.has(scope)) {
				var expiration = Instant.ofEpochSecond(clientScopes.getLong(scope)).truncatedTo(SECONDS);
				if (expiration.isAfter(now)) {
					authorizedScopes.add(scope);
					if (earliestExpiration == null || expiration.isBefore(earliestExpiration)) earliestExpiration = expiration;
					continue;
				}
			}
			unauthorizedScopes.add(scope);
		}
		if (authorizedScopes.isEmpty()) return unauthorized(scopes);

		var authorization = new Authorization(clientId, userId, new AuthorizedScopes(authorizedScopes, earliestExpiration));
		return new AuthResult(authorization.scopes(), unauthorizedScopes, authCode(authorization));
	}

	@Override
	public void nonce(String userId, String clientId, String nonce) {
		var nonceKey = String.join("@", userId, clientId);
		if (nonce != null) {
			nonceMap.put(nonceKey, nonce);
		} else
			nonceMap.remove(nonceKey);
	}

	private AuthResult unauthorized(Collection<String> scopes) {
		return new AuthResult(null, new HashSet<>(scopes), null);
	}

	/*** MailConfig implementation ***/

	@Override
	public Authenticator authenticator() {
		if (auth == null) {
			auth = new Authenticator() {
				// override the getPasswordAuthentication method
				protected PasswordAuthentication getPasswordAuthentication() {
					return new PasswordAuthentication(senderAddress(), senderPassword());
				}
			};
		}
		return auth;
	}

	private String mailConfig(String key) {
		if (!json.has(MAILCONFIG)) return "";
		var config = json.getJSONObject(MAILCONFIG);
		if (config.has(key)) return config.getString(key);
		return "";
	}

	private FileStore mailConfig(String key, Object newValue) {
		if (!json.has(MAILCONFIG)) json.put(MAILCONFIG, new JSONObject());
		var config = json.getJSONObject(MAILCONFIG);
		config.put(key, newValue);
		auth = null;
		return this;
	}

	@Override
	public String smtpHost() {
		return mailConfig(SMTP_HOST);
	}


	@Override
	public MailConfig smtpHost(String newValue) {
		return mailConfig(SMTP_HOST, newValue);
	}

	@Override
	public int smtpPort() {
		if (!json.has(MAILCONFIG)) return 0;
		var config = json.getJSONObject(MAILCONFIG);
		return config.has(SMTP_PORT) ? config.getInt(SMTP_PORT) : 0;
	}

	@Override
	public MailConfig smtpPort(int newValue) {
		return mailConfig(SMTP_PORT, newValue);
	}

	@Override
	public String senderAddress() {
		return mailConfig(SMTP_USER);
	}

	@Override
	public MailConfig senderAddress(String newValue) {
		return mailConfig(SMTP_USER, newValue);
	}

	@Override
	public String senderPassword() {
		return mailConfig(SMTP_PASSWORD);
	}

	@Override
	public MailConfig senderPassword(String newValue) {
		return mailConfig(SMTP_PASSWORD, newValue);
	}

	@Override
	public boolean startTls() {
		if (!json.has(MAILCONFIG)) return false;
		var config = json.getJSONObject(MAILCONFIG);
		return config.has(START_TLS) ? config.getBoolean(START_TLS) : false;
	}

	@Override
	public MailConfig startTls(boolean newValue) {
		return mailConfig(START_TLS, newValue);
	}

	@Override
	public boolean smtpAuth() {
		if (!json.has(MAILCONFIG)) return false;
		var config = json.getJSONObject(MAILCONFIG);
		return config.has(SMTP_AUTH) ? config.getBoolean(SMTP_AUTH) : false;
	}

	@Override
	public MailConfig smtpAuth(boolean newValue) {
		return mailConfig(SMTP_AUTH, newValue);
	}
}
