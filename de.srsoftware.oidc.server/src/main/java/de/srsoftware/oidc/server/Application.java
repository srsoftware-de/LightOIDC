/* © SRSoftware 2024 */
package de.srsoftware.oidc.server;


import com.sun.net.httpserver.HttpServer;
import de.srsoftware.oidc.web.StaticPages;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

public class Application {
	public static void main(String[] args) throws Exception {
		HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
		new StaticPages().bindPath("/static").on(server);
		new LanguageDirector("/static").bindPath("/").on(server);
		server.setExecutor(Executors.newCachedThreadPool());
		server.start();
	}
}
