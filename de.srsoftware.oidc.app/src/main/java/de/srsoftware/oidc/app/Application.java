/* © SRSoftware 2024 */
package de.srsoftware.oidc.app;


import com.sun.net.httpserver.HttpServer;
import de.srsoftware.oidc.backend.Backend;
import de.srsoftware.oidc.web.Forward;
import de.srsoftware.oidc.web.StaticPages;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

public class Application {
	public static final String STATIC_PATH = "/web";
	public static final String INDEX       = STATIC_PATH + "/index.html";

	public static void main(String[] args) throws Exception {
		HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
		new StaticPages().bindPath(STATIC_PATH).on(server);
		new Forward(INDEX).bindPath("/").on(server);
		new Backend().bindPath("/api").on(server);
		server.setExecutor(Executors.newCachedThreadPool());
		server.start();
	}
}
