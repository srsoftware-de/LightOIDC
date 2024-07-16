/* © SRSoftware 2024 */
package de.srsoftware.oidc.server;

import de.srsoftware.oidc.light.LightOICD;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;

public class Application {
	private static Server webserver;

	public static void main(String[] args) throws Exception {
		webserver = new Server(8080);

		ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
		context.setContextPath("/web");
		context.addServlet(LightOICD.class, "/");

		webserver.setHandler(context);
		webserver.start();
		webserver.join();
	}
}
