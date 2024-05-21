/* © SRSoftware 2024 */
package de.srsoftware.oidc.light;

import static de.srsoftware.oidc.light.Constants.*;
import static de.srsoftware.oidc.light.Templates.braced;

import de.srsoftware.oidc.api.User;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
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
		var uri = req.getRequestURI();
		var path = Arrays.stream(uri.split("/")).skip(1).collect(Collectors.toList());
		if (path.isEmpty()) {
			path.add(PAGE_START);
		}

		var optUser = loadUser(req);
		handleGet(path, optUser, req, resp).ifPresent(resp.getWriter()::println);
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		var uri = req.getRequestURI();
		var path = Arrays.stream(uri.split("/")).skip(1).collect(Collectors.toList());
		if (path.isEmpty()) {
			path.add(PAGE_START);
		}

		var optUser = loadUser(req);
		handlePost(path, optUser, req, resp).ifPresent(resp.getWriter()::println);
	}

	private Optional<String> handleGet(List<String> path, Optional<User> optUser, HttpServletRequest req, HttpServletResponse resp) {
		String token = path.remove(0);
		if (optUser.isPresent()) {
			var user = optUser.get();
			switch (token) {
				case PAGE_START:
					return pageStart(user, req, resp);
				case PAGE_WELCOME:
					return pageWelcome(user,req, resp);
			}
		}

		switch (token) {
			case PAGE_LOGIN:
				return pageLogin(req, resp);
			case PAGE_START:
			case PAGE_WELCOME:
				return redirect(resp, PAGE_LOGIN);
		}
		return templates.message(ERR_PAGE_NOT_FOUND);
	}



	private Optional<String> handlePost(List<String> path, Optional<User> optUser, HttpServletRequest req, HttpServletResponse resp) {
		String token = path.remove(0);
		if (optUser.isPresent()) {
			var user = optUser.get();
			switch (token) {
				case PAGE_START:
					return pageStart(user, req, resp);
			}
		}

		switch (token) {
			case PAGE_LOGIN:
				return postLogin(req, resp);
			case PAGE_START:
				return redirect(resp, PAGE_LOGIN);
		}
		return templates.message(ERR_PAGE_NOT_FOUND);
	}

	private Optional<User> loadUser(HttpServletRequest req) {
		HttpSession session = req.getSession();
		if (session.getAttribute(USER) instanceof User user) {
			return Optional.of(user);
		}
		return Optional.empty();
	}



	private Optional<String> pageLogin(HttpServletRequest req, HttpServletResponse resp) {
		LOG.debug("pageLogin(…)");
		try {
			var title = templates.message(TITLE_LOGIN).orElse(TITLE_LOGIN);
			var head = templates.get("head.snippet", Map.of(TITLE, title)).get();
			var login = templates.get("login.snippet", Map.of(USER, "Darling", EMAIL, "", PASSWORD, "")).get();
			var page = templates.get("scaffold.html", Map.of(BODY, login, HEAD, head)).get();
			resp.setContentType("text/html");
			resp.getWriter().println(page);
			return Optional.empty();
		} catch (Exception e) {
			return Optional.of(e.getMessage());
		}
	}

	private Optional<String> pageStart(User user, HttpServletRequest req, HttpServletResponse resp) {
		LOG.debug("pageStart(…)");
		return Optional.empty();
	}

	private Optional<String> pageWelcome(User user, HttpServletRequest req, HttpServletResponse resp) {
		LOG.debug("pageWelcome(…)");
		try {
			var title = templates.message(TITLE_WELCOME).orElse(TITLE_WELCOME);
			var head = templates.get("head.snippet", Map.of(TITLE, title)).get();
			var login = templates.get("welcome.snippet", Map.of(USER, "Darling", EMAIL, "", PASSWORD, "")).get();
			var page = templates.get("scaffold.html", Map.of(BODY, login, HEAD, head)).get();
			resp.setContentType("text/html");
			resp.getWriter().println(page);
			return Optional.empty();
		} catch (Exception e) {
			return Optional.of(e.getMessage());
		}
	}

	private Optional<String> postLogin(HttpServletRequest req, HttpServletResponse resp) {
		LOG.debug("postLogin(…)");
		var email = req.getParameter(EMAIL);
		if (braced(EMAIL).equals(email)) email = "";
		var pass = req.getParameter(PASSWORD);
		var user = tryLogin(email, pass);
		if (user.isPresent()) {
			req.getSession().setAttribute(USER, user.get());
			return redirect(resp, PAGE_WELCOME);
		}
		try {
			var title = templates.message(TITLE_LOGIN).orElse(TITLE_LOGIN);
			var head = templates.get("head.snippet", Map.of(TITLE, title)).get();
			var login = templates.get("login.snippet", Map.of(USER, "Darling", EMAIL, email, PASSWORD, "")).get();
			var page = templates.get("scaffold.html", Map.of(BODY, login, HEAD, head)).get();
			resp.setContentType("text/html");
			resp.getWriter().println(page);
			return Optional.empty();
		} catch (Exception e) {
			return Optional.of(e.getMessage());
		}
	}

	private Optional<User> tryLogin(String email, String pass) {
		if (email == null || pass == null) return Optional.empty();
		if (email.equals(pass)) return Optional.of(new User());
		return Optional.empty();
	}

	private Optional<String> redirect(HttpServletResponse resp, String path) {
		try {
			resp.sendRedirect(path);
		} catch (IOException e) {
			return templates.message(ERR_REDIRECT_FAILED, Map.of(TARGET, path));
		}
		return Optional.empty();
	}
}
