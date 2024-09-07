/* © SRSoftware 2024 */
package de.srsoftware.oidc.app;


import static de.srsoftware.oidc.api.Constants.*;
import static de.srsoftware.oidc.api.data.Permission.*;
import static de.srsoftware.utils.Optionals.emptyIfBlank;
import static de.srsoftware.utils.Paths.configDir;
import static de.srsoftware.utils.Paths.extension;
import static de.srsoftware.utils.Strings.uuid;
import static java.lang.System.Logger.Level.DEBUG;
import static java.lang.System.Logger.Level.ERROR;
import static java.lang.System.getenv;
import static java.util.Optional.empty;

import com.sun.net.httpserver.HttpServer;
import de.srsoftware.logging.ColorLogger;
import de.srsoftware.oidc.api.*;
import de.srsoftware.oidc.api.data.User;
import de.srsoftware.oidc.backend.*;
import de.srsoftware.oidc.datastore.file.FileStore;
import de.srsoftware.oidc.datastore.file.FileStoreProvider;
import de.srsoftware.oidc.datastore.file.PlaintextKeyStore;
import de.srsoftware.oidc.datastore.file.UuidHasher;
import de.srsoftware.oidc.datastore.sqlite.*;
import de.srsoftware.oidc.web.Forward;
import de.srsoftware.oidc.web.StaticPages;
import java.io.File;
import java.net.InetSocketAddress;
import java.nio.file.Path;
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

	private static final String	  BASE_PATH	     = "basePath";
	private static final String	  FAVICON	     = "/favicon.ico";
	private static final String	  INDEX	     = STATIC_PATH + "/index.html";
	private static final String	  WELL_KNOWN	     = "/.well-known";
	private static System.Logger	  LOG	     = new ColorLogger("Application").setLogLevel(DEBUG);
	private static ConnectionProvider connectionProvider = new ConnectionProvider();
	public static void	  main(String[] args) throws Exception {

		var            argMap     = map(args);
		Optional<Path> basePath   = argMap.get(BASE_PATH) instanceof Path p ? Optional.of(p) : empty();
		var            configFile = (argMap.get(CONFIG_PATH) instanceof Path p ? p : configDir(APP_NAME).resolve("config.json")).toFile();
		var            config     = new Configuration(configFile);
		var defaultConfigDir = configDir(APP_NAME);
		var passwordHasher = new UuidHasher();
		var firstHash	  = passwordHasher.hash(FIRST_USER_PASS, FIRST_UUID);
		var firstUser	  = new User(FIRST_USER, firstHash, FIRST_USER, "%s@internal".formatted(FIRST_USER), FIRST_UUID).add(MANAGE_CLIENTS, MANAGE_PERMISSIONS, MANAGE_SMTP, MANAGE_USERS);
		var defaultFile = defaultConfigDir.resolve("data.json");
		var        keyStorageLocation = new File(config.getOrDefault("key_storage", defaultConfigDir.resolve("keys")));
		KeyStorage keyStore;
		if ((keyStorageLocation.exists() && keyStorageLocation.isDirectory()) || !keyStorageLocation.getName().contains(".")) {
		   keyStore = new PlaintextKeyStore(keyStorageLocation.toPath());
		} else {  // SQLite
		   var conn = connectionProvider.get(keyStorageLocation);
		   keyStore = new SqliteKeyStore(conn);
		}

		KeyManager keyManager  = new RotatingKeyManager(keyStore);
		FileStoreProvider fileStoreProvider = new FileStoreProvider(passwordHasher);

		var userStorageLocation = new File(config.getOrDefault("user_storage",defaultFile));
		var userService = switch (extension(userStorageLocation).toLowerCase()){
		   case "db", "sqlite", "sqlite3" -> new SqliteUserService(connectionProvider.get(userStorageLocation));
		   default -> fileStoreProvider.get(userStorageLocation);
		};
		userService.init(firstUser);

		var mailConfigLocation = new File(config.getOrDefault("mail_config_storage",defaultFile));
		var mailConfig = switch (extension(mailConfigLocation)){
			case "db", "sqlite", "sqlite3" -> new SqliteMailConfig(connectionProvider.get(userStorageLocation));
			default -> fileStoreProvider.get(mailConfigLocation);
		};

		var sessionStore = new File(config.getOrDefault("session_storage",defaultFile));
		var sessionService = switch (extension(sessionStore)){
			case "db", "sqlite", "sqlite3" -> new SqliteSessionService(connectionProvider.get(sessionStore));
			default -> fileStoreProvider.get(sessionStore);
		};

		var authServiceLocation = new File(config.getOrDefault("auth_store",defaultFile));
		AuthorizationService authService = switch (extension(authServiceLocation)){
			case "db", "sqlite", "sqlite3" -> new SqliteAuthService(connectionProvider.get(sessionStore));
			default -> fileStoreProvider.get(sessionStore);
		};

		var clientStore = new File(config.getOrDefault("client_store",defaultFile));
		ClientService clientService = switch (extension(clientStore)){
			case "db", "sqlite", "sqlite3" -> new SqliteClientService(connectionProvider.get(sessionStore));
			default -> fileStoreProvider.get(sessionStore);
		};

		HttpServer server      = HttpServer.create(new InetSocketAddress(8080), 0);
		var        staticPages = (StaticPages) new StaticPages(basePath).bindPath(STATIC_PATH, FAVICON).on(server);
		new Forward(INDEX).bindPath(ROOT).on(server);
		new WellKnownController().bindPath(WELL_KNOWN).on(server);
		new UserController(mailConfig, sessionService, userService, staticPages).bindPath(API_USER).on(server);
		var tokenControllerConfig = new TokenController.Configuration("https://lightoidc.srsoftware.de", 10);  // TODO configure or derive from hostname
		new TokenController(authService, clientService, keyManager, userService, tokenControllerConfig).bindPath(API_TOKEN).on(server);
		new ClientController(authService, clientService, sessionService).bindPath(API_CLIENT).on(server);
		new KeyStoreController(keyStore).bindPath(JWKS).on(server);
		new EmailController(mailConfig, sessionService).bindPath(API_EMAIL).on(server);
		server.setExecutor(Executors.newCachedThreadPool());
		server.start();
	}

	private static Map<String, Object> map(String[] args) {
		var tokens = new ArrayList<>(List.of(args));
		var map    = new HashMap<String, Object>();

		emptyIfBlank(getenv(BASE_PATH)).map(Path::of).ifPresent(path -> map.put(BASE_PATH, path));
		emptyIfBlank(getenv(CONFIG_PATH)).map(Path::of).ifPresent(path -> map.put(CONFIG_PATH, path));

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
