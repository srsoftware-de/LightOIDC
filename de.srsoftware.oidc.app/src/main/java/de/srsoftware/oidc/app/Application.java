/* © SRSoftware 2024 */
package de.srsoftware.oidc.app;


import static de.srsoftware.oidc.api.Constants.*;
import static de.srsoftware.oidc.api.Permission.MANAGE_CLIENTS;
import static de.srsoftware.utils.Optionals.nonEmpty;
import static de.srsoftware.utils.Paths.configDir;
import static java.lang.System.Logger.Level.DEBUG;
import static java.lang.System.Logger.Level.ERROR;
import static java.lang.System.getenv;

import com.sun.net.httpserver.HttpServer;
import de.srsoftware.logging.ColorLogger;
import de.srsoftware.oidc.api.KeyManager;
import de.srsoftware.oidc.api.KeyStorage;
import de.srsoftware.oidc.api.User;
import de.srsoftware.oidc.backend.*;
import de.srsoftware.oidc.datastore.file.FileStore;
import de.srsoftware.oidc.datastore.file.PlaintextKeyStore;
import de.srsoftware.oidc.datastore.file.UuidHasher;
import de.srsoftware.oidc.web.Forward;
import de.srsoftware.oidc.web.StaticPages;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.Executors;

public class Application {
	public static final String  API_CLIENT      = "/api/client";
	private static final String API_TOKEN       = "/api/token";
	public static final String  API_USER        = "/api/user";
	public static final String  FIRST_USER      = "admin";
	public static final String  FIRST_USER_PASS = "admin";
	public static final String  FIRST_UUID      = UUID.randomUUID().toString();
	public static final String  JWKS            = "/api/jwks";
	public static final String  ROOT            = "/";
	public static final String  STATIC_PATH     = "/web";

	private static final String  BASE_PATH  = "basePath";
	private static final String  FAVICON    = "/favicon.ico";
	private static final String  INDEX      = STATIC_PATH + "/index.html";
	private static final String  WELL_KNOWN = "/.well-known";
	private static System.Logger LOG        = new ColorLogger("Application").setLogLevel(DEBUG);

	public static void main(String[] args) throws Exception {
		var            argMap         = map(args);
		Optional<Path> basePath       = argMap.get(BASE_PATH) instanceof Path p ? Optional.of(p) : Optional.empty();
		var            storageFile    = (argMap.get(CONFIG_PATH) instanceof Path p ? p : configDir(APP_NAME).resolve("config.json")).toFile();
		var            keyDir         = storageFile.getParentFile().toPath().resolve("keys");
		var            passwordHasher = new UuidHasher();
		var            firstHash      = passwordHasher.hash(FIRST_USER_PASS, FIRST_UUID);
		var            firstUser      = new User(FIRST_USER, firstHash, FIRST_USER, "%s@internal".formatted(FIRST_USER), FIRST_UUID).add(MANAGE_CLIENTS);
		KeyStorage     keyStore       = new PlaintextKeyStore(keyDir);
		KeyManager     keyManager     = new RotatingKeyManager(keyStore);
		FileStore      fileStore      = new FileStore(storageFile, passwordHasher).init(firstUser);
		HttpServer     server         = HttpServer.create(new InetSocketAddress(8080), 0);
		new StaticPages(basePath).bindPath(STATIC_PATH, FAVICON).on(server);
		new Forward(INDEX).bindPath(ROOT).on(server);
		new WellKnownController().bindPath(WELL_KNOWN).on(server);
		new UserController(fileStore, fileStore).bindPath(API_USER).on(server);
		new TokenController(fileStore, fileStore, keyManager, fileStore).bindPath(API_TOKEN).on(server);
		new ClientController(fileStore, fileStore, fileStore).bindPath(API_CLIENT).on(server);
		new KeyStoreController(keyStore).bindPath(JWKS).on(server);
		// server.setExecutor(Executors.newCachedThreadPool());
		server.setExecutor(Executors.newSingleThreadExecutor());
		server.start();
	}

	private static Map<String, Object> map(String[] args) {
		var tokens = new ArrayList<>(List.of(args));
		var map    = new HashMap<String, Object>();

		nonEmpty(getenv(BASE_PATH)).map(Path::of).ifPresent(path -> map.put(BASE_PATH, path));
		nonEmpty(getenv(CONFIG_PATH)).map(Path::of).ifPresent(path -> map.put(CONFIG_PATH, path));

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
