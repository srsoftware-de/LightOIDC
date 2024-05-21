/* © SRSoftware 2024 */
package de.srsoftware;

import de.srsoftware.oidc.api.User;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@WebServlet(urlPatterns = "/")
public class LightOICD extends HttpServlet {
	private static final Logger LOG = LoggerFactory.getLogger(LightOICD.class);
	private static final Templates templates;
	static {
		try {
			templates = Templates.singleton();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
		var path = Arrays.stream(req.getRequestURI().split("/")).skip(1).toList();
		User user = null;
		if (path.isEmpty()) {
			landingPage(req, resp, user);
			return;
		}
		switch (path.remove(0)) {
			default:
				helloWorld(req, resp, path);
		}
	}

	private void helloWorld(HttpServletRequest req, HttpServletResponse resp, List<String> path) throws IOException {
		LOG.debug("helloWorld(…), path = {}", path);
		resp.setContentType("text/html");
		PrintWriter out = resp.getWriter();
		out.println("<html><head><title>Hello World Servlet</title></head>");
		out.println("<body>");
		out.println("<h1>Hello Guys!</h1>");
		out.println("<ul>");
		path.forEach(part -> out.println("<li>" + part + "</li>"));
		out.println("</ul>");
		out.println("</body></html>");
		out.close();
	}

	private void landingPage(HttpServletRequest req, HttpServletResponse resp, User user) throws IOException {
		LOG.debug("landingPage(…)");
		var index = templates.get("index.html", Map.of("user","Darling"));
		resp.setContentType("text/html");
		resp.getWriter().println(index.get());
	}
}
