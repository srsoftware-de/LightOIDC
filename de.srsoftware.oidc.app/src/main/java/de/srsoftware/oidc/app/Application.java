/* © SRSoftware 2024 */
package de.srsoftware.oidc.app;


import com.sun.net.httpserver.HttpServer;
import de.srsoftware.oidc.api.SessionService;
import de.srsoftware.oidc.api.User;
import de.srsoftware.oidc.api.UserService;
import de.srsoftware.oidc.backend.Backend;
import de.srsoftware.oidc.datastore.file.FileStore;
import de.srsoftware.oidc.datastore.file.UuidHasher;
import de.srsoftware.oidc.web.Forward;
import de.srsoftware.oidc.web.StaticPages;
import java.io.File;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.Executors;

public class Application {
	public static final String  STATIC_PATH     = "/web";
	public static final String  FIRST_USER      = "admin";
	public static final String  FIRST_USER_PASS = "admin";
	public static final String  FIRST_UUID      = UUID.randomUUID().toString();
	public static final String  INDEX           = STATIC_PATH + "/index.html";
	private static final String BASE_PATH       = "basePath";

	public static void main(String[] args) throws Exception {
		var            argMap         = map(args);
		Optional<Path> basePath       = argMap.get(BASE_PATH) instanceof Path p ? Optional.of(p) : Optional.empty();
		var            storageFile    = new File("/tmp/lightoidc.json");
		var            passwordHasher = new UuidHasher();
		var            firstHash      = passwordHasher.hash(FIRST_USER_PASS, FIRST_UUID);
		var            firstUser      = new User(FIRST_USER, firstHash, FIRST_USER, "%s@internal".formatted(FIRST_USER), FIRST_UUID);
		FileStore      fileStore      = new FileStore(storageFile, passwordHasher).init(firstUser);
		UserService    userService    = fileStore;
		SessionService sessionService = fileStore;
		HttpServer     server         = HttpServer.create(new InetSocketAddress(8080), 0);
		new StaticPages(basePath).bindPath(STATIC_PATH).on(server);
		new Forward(INDEX).bindPath("/").on(server);
		new Backend(sessionService, userService).bindPath("/api").on(server);
		server.setExecutor(Executors.newCachedThreadPool());
		server.start();
	}

	private static Map<String, Object> map(String[] args) {
		var tokens = new ArrayList<>(List.of(args));
		var map    = new HashMap<String, Object>();
		while (!tokens.isEmpty()) {
			var token = tokens.remove(0);
			switch (token) {
				case "--base":
					if (tokens.isEmpty()) throw new IllegalArgumentException("--path option requires second argument!");
					map.put(BASE_PATH, Path.of(tokens.remove(0)));
					break;
				default:
					System.err.printf("Unknown option: %s\n", token);
			}
		}

		return map;
	}
}
