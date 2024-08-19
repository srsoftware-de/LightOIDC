/* © SRSoftware 2024 */
package de.srsoftware.oidc.backend;

import static de.srsoftware.oidc.api.Constants.*;
import static de.srsoftware.oidc.api.data.Permission.MANAGE_USERS;
import static de.srsoftware.oidc.api.data.User.*;
import static de.srsoftware.utils.Strings.uuid;
import static java.lang.System.Logger.Level.WARNING;
import static java.net.HttpURLConnection.*;
import static java.nio.charset.StandardCharsets.UTF_8;

import com.sun.net.httpserver.HttpExchange;
import de.srsoftware.http.SessionToken;
import de.srsoftware.oidc.api.*;
import de.srsoftware.oidc.api.data.Session;
import de.srsoftware.oidc.api.data.User;
import jakarta.mail.*;
import jakarta.mail.internet.*;
import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.json.JSONObject;

public class UserController extends Controller {
	private final UserService    users;
	private final MailConfig     mailConfig;
	private final ResourceLoader resourceLoader;

	public UserController(MailConfig mailConfig, SessionService sessionService, UserService userService, ResourceLoader resourceLoader) {
		super(sessionService);
		users	    = userService;
		this.mailConfig	    = mailConfig;
		this.resourceLoader = resourceLoader;
	}

	private boolean addUser(HttpExchange ex, Session session) throws IOException {
		var user = session.user();
		if (!user.hasPermission(MANAGE_USERS)) return sendEmptyResponse(HTTP_FORBIDDEN, ex);
		var json  = json(ex);
		var newID = uuid();
		User.of(json, uuid()).ifPresent(u -> users.updatePassword(u, json.getString(PASSWORD)));
		return sendContent(ex, newID);
	}

	@Override
	public boolean doDelete(String path, HttpExchange ex) throws IOException {
		var optSession = getSession(ex);
		if (optSession.isEmpty()) return sendEmptyResponse(HTTP_UNAUTHORIZED, ex);

		// post-login paths
		var session = optSession.get();
		sessions.extend(session);

		switch (path) {
			case "/delete":
				return deleteUser(ex, session);
		}
		return badRequest(ex, "%s not found".formatted(path));
	}

	private boolean deleteUser(HttpExchange ex, Session session) throws IOException {
		var json = json(ex);
		if (!json.has(USER_ID)) return badRequest(ex, "missing_user_id");
		var uuid = json.getString(USER_ID);
		if (uuid == null || uuid.isBlank()) return badRequest(ex, "missing_user_id");
		if (session.user().uuid().equals(uuid)) return badRequest(ex, "must_not_delete_self");
		if (!json.has(CONFIRMED) || !json.getBoolean(CONFIRMED)) return badRequest(ex, "missing_confirmation");
		Optional<User> targetUser = users.load(uuid);
		if (targetUser.isEmpty()) return badRequest(ex, "unknown_user");
		users.delete(targetUser.get());
		return sendEmptyResponse(HTTP_OK, ex);
	}

	@Override
	public boolean doGet(String path, HttpExchange ex) throws IOException {
		switch (path) {
			case "/info":
				return userInfo(ex);
			case "/reset":
				return generateResetLink(ex);
		}
		var optSession = getSession(ex);
		if (optSession.isEmpty()) return sendEmptyResponse(HTTP_UNAUTHORIZED, ex);

		// post-login paths
		var session = optSession.get();
		sessions.extend(session);

		switch (path) {
			case "/logout":
				return logout(ex, session);
		}

		return notFound(ex);
	}


	@Override
	public boolean doPost(String path, HttpExchange ex) throws IOException {
		switch (path) {
			case "/login":
				return login(ex);
			case "/reset":
				return resetPassword(ex);
		}
		var optSession = getSession(ex);
		if (optSession.isEmpty()) return sendEmptyResponse(HTTP_UNAUTHORIZED, ex);

		// post-login paths
		var session = optSession.get();
		sessions.extend(session);

		switch (path) {
			case "/":
				return sendUserAndCookie(ex, session);
			case "/add":
				return addUser(ex, session);
			case "/list":
				return list(ex, session);
			case "/password":
				return updatePassword(ex, session);
			case "/update":
				return updateUser(ex, session);
		}
		return notFound(ex);
	}


	private boolean generateResetLink(HttpExchange ex) throws IOException {
		var idOrEmail = queryParam(ex).get("user");
		var url       = url(ex)  //
		              .replace("/api/user/", "/web/")
		              .split("\\?")[0] +
		          ".html";
		Set<User> matchingUsers = users.find(idOrEmail);
		if (!matchingUsers.isEmpty()) {
			resourceLoader	//
			    .loadFile(language(ex), "reset_password.template.txt")
			    .map(ResourceLoader.Resource::content)
			    .map(bytes -> new String(bytes, UTF_8))
			    .ifPresent(template -> {  //
				    matchingUsers.forEach(user -> sendResetLink(user, template, url));
			    });
		}
		return sendEmptyResponse(HTTP_OK, ex);
	}


	private boolean list(HttpExchange ex, Session session) throws IOException {
		var user = session.user();
		if (!user.hasPermission(MANAGE_USERS)) return sendEmptyResponse(HTTP_FORBIDDEN, ex);
		var json = new JSONObject();
		users.list().forEach(u -> json.put(u.uuid(), u.map(false)));
		return sendContent(ex, json);
	}

	private boolean login(HttpExchange ex) throws IOException {
		var body = json(ex);

		var username = body.has(USERNAME) ? body.getString(USERNAME) : null;
		var password = body.has(PASSWORD) ? body.getString(PASSWORD) : null;

		Optional<User> user = users.load(username, password);
		if (user.isPresent()) return sendUserAndCookie(ex, sessions.createSession(user.get()));
		return sendEmptyResponse(HTTP_UNAUTHORIZED, ex);
	}

	private boolean logout(HttpExchange ex, Session session) throws IOException {
		sessions.dropSession(session.id());
		new SessionToken("").addTo(ex);
		return sendEmptyResponse(HTTP_OK, ex);
	}


	private boolean resetPassword(HttpExchange ex) throws IOException {
		var data = json(ex);
		if (!data.has(TOKEN)) return sendContent(ex, HTTP_UNAUTHORIZED, "token missing");
		var passwords = data.getJSONArray("newpass");
		var newPass   = passwords.getString(0);
		if (!newPass.equals(passwords.getString(1))) {
			return badRequest(ex, "password mismatch");
		}
		try {
			strong(newPass);
		} catch (RuntimeException e) {
			return sendContent(ex, HTTP_BAD_REQUEST, e.getMessage());
		}
		var token   = data.getString(TOKEN);
		var optUser = users.consumeToken(token);
		if (optUser.isEmpty()) return sendContent(ex, HTTP_UNAUTHORIZED, "invalid token");
		var user = optUser.get();
		users.updatePassword(user, newPass);
		var session = sessions.createSession(user);
		new SessionToken(session.id()).addTo(ex);
		return sendRedirect(ex, "/");
	}


	private void sendResetLink(User user, String template, String url) {
		LOG.log(WARNING, "Sending password link to {0}", user.email());
		var token = users.accessToken(user);

		var parts = template  //
			.replace("{service}", APP_NAME)
			.replace("{displayname}", user.realName())
			.replace("{link}", String.join("?token=", url, token.id()))
			.split("\n", 2);
		var subj    = parts[0];
		var content = parts[1];
		try {
			var     session = jakarta.mail.Session.getDefaultInstance(mailConfig.props(), mailConfig.authenticator());
			Message message = new MimeMessage(session);
			message.setFrom(new InternetAddress(mailConfig.senderAddress()));
			message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(user.email()));
			message.setSubject(subj);


			MimeBodyPart mimeBodyPart = new MimeBodyPart();
			mimeBodyPart.setContent(content, "text/plain; charset=utf-8");

			Multipart multipart = new MimeMultipart();
			multipart.addBodyPart(mimeBodyPart);

			message.setContent(multipart);

			Transport.send(message);
		} catch (AddressException e) {
			throw new RuntimeException(e);
		} catch (MessagingException e) {
			throw new RuntimeException(e);
		}
	}

	private boolean sendUserAndCookie(HttpExchange ex, Session session) throws IOException {
		new SessionToken(session.id()).addTo(ex);
		return sendContent(ex, session.user().map(false));
	}


	private void strong(String pass) {
		var digits  = false;
		var special = false;
		var alpha   = false;
		for (int i = 0; i < pass.length(); i++) {
			char c = pass.charAt(i);
			if (Character.isDigit(c)) {
				digits = true;
			} else if (Character.isAlphabetic(c)) {
				alpha = true;
			} else
				special = true;
		}
		if (pass.length() < 16) {
			if (!alpha) throw new RuntimeException("Passwords shorter than 16 characters must contain alphabetic characters!");
			if (!digits && !special) throw new RuntimeException("Passwords shorter than 16 characters must contain at least one digit or special character!");
			if (pass.length() < 10) {
				if (!digits || !special) throw new RuntimeException("Passwords shorter than 10 characters must contain digits as well as alphabetic and special characters!");
				if (pass.length() < 6) throw new RuntimeException("Password must have at least 6 characters!");
			}
		}
	}

	private boolean updatePassword(HttpExchange ex, Session session) throws IOException {
		var user = session.user();
		var json = json(ex);
		var uuid = json.getString(UUID);
		if (!uuid.equals(user.uuid())) {
			return sendEmptyResponse(HTTP_FORBIDDEN, ex);
		}
		var oldPass = json.getString("oldpass");
		if (!users.passwordMatches(oldPass, user.hashedPassword())) return badRequest(ex, "wrong password");

		var passwords = json.getJSONArray("newpass");
		var newPass   = passwords.getString(0);
		if (!newPass.equals(passwords.getString(1))) {
			return badRequest(ex, "password mismatch");
		}
		try {
			strong(newPass);
		} catch (RuntimeException e) {
			return sendContent(ex, HTTP_BAD_REQUEST, e.getMessage());
		}
		users.updatePassword(user, newPass);
		return sendContent(ex, user.map(false));
	}

	private boolean updateUser(HttpExchange ex, Session session) throws IOException {
		var user = session.user();
		var json = json(ex);
		var uuid = json.getString(UUID);
		if (!uuid.equals(user.uuid())) {
			return sendEmptyResponse(HTTP_FORBIDDEN, ex);
		}
		user.username(json.getString(USERNAME));
		user.email(json.getString(EMAIL));
		user.realName(json.getString(REALNAME));
		user.sessionDuration(Duration.ofMinutes(json.getInt(SESSION_DURATION)));
		users.save(user);
		return sendContent(ex, user.map(false));
	}

	private boolean userInfo(HttpExchange ex) throws IOException {
		var optUser = getBearer(ex).flatMap(users::forToken);
		if (optUser.isEmpty()) return sendEmptyResponse(HTTP_UNAUTHORIZED, ex);
		var user = optUser.get();
		var map  = Map.of("sub", user.uuid(), "email", user.email());
		return sendContent(ex, new JSONObject(map));
	}
}
