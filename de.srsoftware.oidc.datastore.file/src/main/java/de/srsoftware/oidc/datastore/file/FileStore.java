/* © SRSoftware 2024 */
package de.srsoftware.oidc.datastore.file; /* © SRSoftware 2024 */
import static de.srsoftware.oidc.api.Constants.*;
import static de.srsoftware.oidc.api.data.User.*;
import static de.srsoftware.utils.Optionals.nullable;
import static de.srsoftware.utils.Strings.uuid;
import static java.lang.System.Logger.Level.WARNING;
import static java.util.Optional.empty;

import de.srsoftware.oidc.api.*;
import de.srsoftware.oidc.api.data.*;
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
	private static final String	   CODES	  = "codes";
	private static final String	   REDIRECT_URIS  = "redirect_uris";
	private static final String	   SESSIONS	  = "sessions";
	private static final String	   USERS	  = "users";
	private static final List<String> KEYS	  = List.of(USERNAME, EMAIL, REALNAME);

	private final Path       storageFile;
	private final JSONObject json;
	private final PasswordHasher<String> passwordHasher;
	private Duration	     sessionDuration = Duration.of(10, ChronoUnit.MINUTES);
	private Map<String, Client>	     clients	     = new HashMap<>();
	private Map<String, User>	     accessTokens    = new HashMap<>();
	private Map<String, Authorization>   authCodes	     = new HashMap<>();
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

	public FileStore save() {
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
		if (!json.has(MAILCONFIG)) json.put(MAILCONFIG, new JSONObject());
		if (!json.has(SESSIONS)) json.put(SESSIONS, new JSONObject());
		if (!json.has(USERS)) save(defaultUser);
		return this;
	}

	@Override
	public Set<User> find(String key) {
		var users  = json.getJSONObject(USERS);
		var result = new HashSet<User>();
		for (var id : users.keySet()) {
			var data = users.getJSONObject(id);
			if (id.equals(key)) User.of(data, id).ifPresent(result::add);
			if (KEYS.stream().map(data::getString).anyMatch(val -> val.equals(key))) User.of(data, id).ifPresent(result::add);
		}
		return result;
	}

	@Override
	public List<User> list() {
		var        users  = json.getJSONObject(USERS);
		List<User> result = new ArrayList<>();
		for (var uid : users.keySet()) User.of(users.getJSONObject(uid), uid).ifPresent(result::add);
		return result;
	}


	@Override
	public Optional<User> load(String userId) {
		try {
			var users    = json.getJSONObject(USERS);
			var userData = users.getJSONObject(userId);
			return User.of(userData, userId);
		} catch (Exception ignored) {
		}
		return empty();
	}

	@Override
	public Optional<User> load(String user, String password) {
		try {
			var users = json.getJSONObject(USERS);
			var uuids = users.keySet();
			for (String userId : uuids) {
				var userData = users.getJSONObject(userId);

				if (KEYS.stream().map(userData::getString).noneMatch(val -> val.equals(user))) continue;
				var hashedPass = userData.getString(PASSWORD);
				if (passwordHasher.matches(password, hashedPass)) return User.of(userData, userId);
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
		return save(user.hashedPassword(passwordHasher.hash(plaintextPassword, uuid())));
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
	public AuthorizationService authorize(User user, Client client, Collection<String> scopes, Instant expiration) {
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
		var config = json.getJSONObject(MAILCONFIG);
		if (config.has(key)) return config.getString(key);
		return "";
	}

	private FileStore mailConfig(String key, Object newValue) {
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
		var config = json.getJSONObject(MAILCONFIG);
		return config.has(START_TLS) ? config.getBoolean(START_TLS) : false;
	}

	@Override
	public MailConfig startTls(boolean newValue) {
		return mailConfig(START_TLS, newValue);
	}

	@Override
	public boolean smtpAuth() {
		var config = json.getJSONObject(MAILCONFIG);
		return config.has(SMTP_AUTH) ? config.getBoolean(SMTP_AUTH) : false;
	}

	@Override
	public MailConfig smtpAuth(boolean newValue) {
		return mailConfig(SMTP_AUTH, newValue);
	}
}
