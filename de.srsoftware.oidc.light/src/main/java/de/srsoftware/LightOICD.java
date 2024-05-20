/* © SRSoftware 2024 */
package de.srsoftware;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
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

	@Override
	public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
		var path = Arrays.stream(req.getRequestURI().split("/")).skip(1).toList();
		resp.setContentType("text/html");
		PrintWriter out = resp.getWriter();
		out.println("<html><head><title>Hello World Servlet</title></head>");
		out.println("<body>");
		out.println("<h1>Hello Guys!</h1>");
		out.println("<ul>");
		path.forEach(part -> out.println("<li>"+part+"</li>"));
		out.println("</ul>");
		out.println("</body></html>");
		out.close();
	}
}
