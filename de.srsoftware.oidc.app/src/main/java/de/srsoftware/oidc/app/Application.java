/* © SRSoftware 2025 */
package de.srsoftware.oidc.app;


import static de.srsoftware.oidc.api.Constants.*;
import static de.srsoftware.oidc.api.data.Permission.*;
import static de.srsoftware.tools.Optionals.absentIfBlank;
import static de.srsoftware.tools.Optionals.nullable;
import static de.srsoftware.tools.Paths.configDir;
import static de.srsoftware.tools.Strings.uuid;
import static java.lang.System.Logger.Level.ERROR;
import static java.lang.System.Logger.Level.INFO;
import static java.lang.System.getenv;
import static java.util.Optional.empty;

import com.sun.net.httpserver.HttpServer;
import de.srsoftware.oidc.api.*;
import de.srsoftware.oidc.api.data.User;
import de.srsoftware.oidc.backend.*;
import de.srsoftware.oidc.datastore.encrypted.EncryptedClientService;
import de.srsoftware.oidc.datastore.encrypted.EncryptedKeyStore;
import de.srsoftware.oidc.datastore.encrypted.EncryptedMailConfig;
import de.srsoftware.oidc.datastore.encrypted.EncryptedUserService;
import de.srsoftware.oidc.datastore.file.FileStoreProvider;
import de.srsoftware.oidc.datastore.file.PlaintextKeyStore;
import de.srsoftware.oidc.web.Forward;
import de.srsoftware.oidc.web.StaticPages;
import de.srsoftware.tools.UuidHasher;
import java.io.File;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.Executors;

public class Application {
	public static final String  API_CLIENT      = "/api/client";
	private static final String API_TOKEN       = "/api/token";
	public static final String  API_USER        = "/api/user";
	public static final String  API_EMAIL       = "/api/email";
	public static final String  FIRST_USER      = "admin";
	public static final String  FIRST_USER_PASS = "admin";
	public static final String  FIRST_UUID      = uuid();
	public static final String  JWKS            = "/api/jwks.json";
	public static final String  ROOT            = "/";
	public static final String  STATIC_PATH     = "/web";

	private static final String BASE_PATH  = "basePath";
	private static final String FAVICON    = "/favicon.ico";
	private static final String INDEX      = STATIC_PATH + "/index.html";
	private static final String WELL_KNOWN = "/.well-known";
	private static final System.Logger LOG = System.getLogger("Application");  // new ColorLogger("Application").setLogLevel(DEBUG);

	public static void main(String[] args) throws Exception {
		LOG.log(INFO,"Starting LightOIDC…");
		var            argMap        = map(args);
		Optional<Path> basePath      = argMap.get(BASE_PATH) instanceof Path p ? Optional.of(p) : empty();
		var            configDir     = configDir(APP_NAME);
		var            defaultFile   = configDir.resolve("data.json");
		var            configFile    = (argMap.get(CONFIG_PATH) instanceof Path p ? p : configDir.resolve("config.json")).toFile();
		var            config        = new Configuration(configFile);
		var            encryptionKey = nullable(System.getenv(ENCRYPTION_KEY)).or(() -> config.get(ENCRYPTION_KEY));
		var            passHasher    = new UuidHasher();
		var            firstHash     = passHasher.hash(FIRST_USER_PASS, FIRST_UUID);
		var            firstUser     = new User(FIRST_USER, firstHash, FIRST_USER, "%s@internal".formatted(FIRST_USER), FIRST_UUID).add(MANAGE_CLIENTS, MANAGE_PERMISSIONS, MANAGE_SMTP, MANAGE_USERS);


		if (encryptionKey.isPresent()) LOG.log(INFO,"Encryption key is set, using encrypted database.");

		FileStoreProvider fileStoreProvider = new FileStoreProvider(passHasher);
		var	  userService	    = setupUserService(config, encryptionKey, defaultFile, fileStoreProvider, passHasher).init(firstUser);
		var	  sessionService    = setupSessionService(config, defaultFile, fileStoreProvider);
		var	  mailConfig	    = setupMailConfig(config, encryptionKey, defaultFile, fileStoreProvider);
		var	  keyStore	    = setupKeyStore(config, encryptionKey, configDir);
		KeyManager	  keyManager	    = new RotatingKeyManager(keyStore);
		var	  authService	    = setupAuthService(config, defaultFile, fileStoreProvider);
		var	  clientService	    = setupClientService(config, encryptionKey, defaultFile, fileStoreProvider);
		HttpServer	  server	    = HttpServer.create(new InetSocketAddress(8080), 0);
		var	  staticPages	    = (StaticPages) new StaticPages(basePath).bindPath(STATIC_PATH, FAVICON).on(server);
		new Forward(INDEX).bindPath(ROOT).on(server);
		new WellKnownController().bindPath(WELL_KNOWN, "/realms/oidc" + WELL_KNOWN).on(server);
		new UserController(mailConfig, sessionService, userService, staticPages).bindPath(API_USER).on(server);
		var tokenControllerConfig = new TokenController.Configuration(10);
		var tokenController       = new TokenController(authService, clientService, keyManager, userService, tokenControllerConfig);
		tokenController.bindPath(API_TOKEN).on(server);
		new ClientController(authService, clientService, sessionService, userService, tokenController).bindPath(API_CLIENT).on(server);
		new KeyStoreController(keyStore).bindPath(JWKS).on(server);
		new EmailController(mailConfig, sessionService, userService).bindPath(API_EMAIL).on(server);
		server.setExecutor(Executors.newCachedThreadPool());
		server.start();
	}

	private static ClientService setupClientService(Configuration config, Optional<String> encryptionKey, Path defaultFile, FileStoreProvider fileStoreProvider) throws SQLException {
		var           clientStore   = new File(config.getOrDefault("client_store", defaultFile));
		ClientService clientService = fileStoreProvider.get(clientStore);

		if (encryptionKey.isPresent()) {
			var salt      = config.getOrDefault(SALT, uuid());
			clientService = new EncryptedClientService(encryptionKey.get(), salt, clientService);
		}
		return clientService;
	}

	private static AuthorizationService setupAuthService(Configuration config, Path defaultFile, FileStoreProvider fileStoreProvider) throws SQLException {
		var authServiceLocation = new File(config.getOrDefault("auth_store", defaultFile));
		return fileStoreProvider.get(authServiceLocation);
	}

	private static SessionService setupSessionService(Configuration config, Path defaultFile, FileStoreProvider fileStoreProvider) {
		var sessionStore = new File(config.getOrDefault("session_storage", defaultFile));
		return fileStoreProvider.get(sessionStore);
	}

	private static MailConfig setupMailConfig(Configuration config, Optional<String> encryptionKey, Path defaultFile, FileStoreProvider fileStoreProvider) throws SQLException {
		var        mailConfigLocation = new File(config.getOrDefault("mail_config_storage", defaultFile));
		MailConfig mailConfig         = fileStoreProvider.get(mailConfigLocation);

		if (encryptionKey.isPresent()) {
			var salt   = config.getOrDefault(SALT, uuid());
			mailConfig = new EncryptedMailConfig(mailConfig, encryptionKey.get(), salt);
		}
		return mailConfig;
	}

	private static UserService setupUserService(Configuration config, Optional<String> encryptionKey, Path defaultFile, FileStoreProvider fileStoreProvider, UuidHasher passHasher) throws SQLException {
		var         userStorageLocation = new File(config.getOrDefault("user_storage", defaultFile));
		UserService userService	= fileStoreProvider.get(userStorageLocation);

		if (encryptionKey.isPresent()) {
			var salt    = config.getOrDefault(SALT, uuid());
			userService = new EncryptedUserService(userService, encryptionKey.get(), salt, passHasher);
		}
		return userService;
	}

	private static KeyStorage setupKeyStore(Configuration config, Optional<String> encryptionKey, Path defaultConfigDir) throws SQLException {
		var        keyStorageLocation = new File(config.getOrDefault("key_storage", defaultConfigDir.resolve("keys")));
		KeyStorage keyStore           = new PlaintextKeyStore(keyStorageLocation.toPath());

		if (encryptionKey.isPresent()) {
			var salt = config.getOrDefault(SALT, uuid());
			keyStore = new EncryptedKeyStore(encryptionKey.get(), salt, keyStore);
		}
		return keyStore;
	}

	private static Map<String, Object> map(String[] args) {
		var tokens = new ArrayList<>(List.of(args));
		var map    = new HashMap<String, Object>();

		absentIfBlank(getenv(BASE_PATH)).map(Path::of).ifPresent(path -> map.put(BASE_PATH, path));
		absentIfBlank(getenv(CONFIG_PATH)).map(Path::of).ifPresent(path -> map.put(CONFIG_PATH, path));

		// Command line arguments override environment
		while (!tokens.isEmpty()) {
			var token = tokens.remove(0);
			switch (token) {
				case "--base":
					if (tokens.isEmpty()) throw new IllegalArgumentException("--base option requires second argument!");
					map.put(BASE_PATH, Path.of(tokens.remove(0)));
					break;
				case "--config":
					if (tokens.isEmpty()) throw new IllegalArgumentException("--config option requires second argument!");
					map.put(CONFIG_PATH, Path.of(tokens.remove(0)));
					break;
				default:
					LOG.log(ERROR, "Unknown option: {0}", token);
			}
		}

		return map;
	}
}
