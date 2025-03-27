/* © SRSoftware 2025 */
package de.srsoftware.oidc.backend;

import static de.srsoftware.oidc.api.Constants.*;
import static de.srsoftware.oidc.api.data.Permission.MANAGE_USERS;
import static de.srsoftware.oidc.api.data.User.*;
import static de.srsoftware.tools.Strings.uuid;
import static java.lang.System.Logger.Level.WARNING;
import static java.net.HttpURLConnection.*;
import static java.nio.charset.StandardCharsets.UTF_8;

import com.sun.net.httpserver.HttpExchange;
import de.srsoftware.oidc.api.*;
import de.srsoftware.oidc.api.data.Permission;
import de.srsoftware.oidc.api.data.Session;
import de.srsoftware.oidc.api.data.User;
import de.srsoftware.tools.Path;
import de.srsoftware.tools.SessionToken;
import de.srsoftware.tools.result.*;
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

	private boolean addUser(HttpExchange ex, User user) throws IOException {
		if (!user.hasPermission(MANAGE_USERS)) return sendEmptyResponse(HTTP_FORBIDDEN, ex);
		var json  = json(ex);
		var newID = uuid();
		User.of(json, uuid()).ifPresent(newUser -> {
			users.save(newUser);
			users.updatePassword(newUser, json.getString(PASSWORD));
		});
		return sendContent(ex, newID);
	}

	@Override
	public boolean doDelete(Path path, HttpExchange ex) throws IOException {
		var optSession = getSession(ex);
		if (optSession.isEmpty()) return sendEmptyResponse(HTTP_UNAUTHORIZED, ex);
		var session = optSession.get();
		var optUser = users.load(session.userId());
		if (optUser.isEmpty()) return invalidSessionUser(ex);
		var user = optUser.get();
		sessions.extend(session, user);

		switch (path.pop()) {
			case "delete":
				return deleteUser(ex, user);
			case "permission":
				return editPermission(ex, user, true);
		}
		return badRequest(ex, "%s not found".formatted(path));
	}

	private boolean deleteUser(HttpExchange ex, User user) throws IOException {
		if (!user.hasPermission(MANAGE_USERS)) return sendEmptyResponse(HTTP_FORBIDDEN, ex);
		var json = json(ex);
		if (!json.has(USER_ID)) return badRequest(ex, "missing_user_id");
		var uuid = json.getString(USER_ID);
		if (uuid == null || uuid.isBlank()) return badRequest(ex, "missing_user_id");
		if (user.uuid().equals(uuid)) return badRequest(ex, "must_not_delete_self");
		if (!json.has(CONFIRMED) || !json.getBoolean(CONFIRMED)) return badRequest(ex, "missing_confirmation");
		Optional<User> targetUser = users.load(uuid);
		if (targetUser.isEmpty()) return badRequest(ex, "unknown_user");
		users.delete(targetUser.get());
		return sendEmptyResponse(HTTP_OK, ex);
	}

	@Override
	public boolean doGet(Path path, HttpExchange ex) throws IOException {
		var part = path.pop();
		switch (part) {
			case "info":
				return userInfo(ex);
			case "reset":
				return generateResetLink(ex);
		}
		var optSession = getSession(ex);
		if (optSession.isEmpty()) return sendEmptyResponse(HTTP_UNAUTHORIZED, ex);
		var session = optSession.get();
		var optUser = users.load(session.userId());
		if (optUser.isEmpty()) return invalidSessionUser(ex);
		var user = optUser.get();
		sessions.extend(session, user);

		switch (part) {
			case "logout":
				return logout(ex, session);
		}

		return notFound(ex);
	}


	@Override
	public boolean doPost(Path pathstack, HttpExchange ex) throws IOException {
		var path = pathstack.toString();
		switch (path) {
			case "login":
				return login(ex);
			case "reset":
				return resetPassword(ex);
		}
		var optSession = getSession(ex);
		if (optSession.isEmpty()) return sendEmptyResponse(HTTP_UNAUTHORIZED, ex);
		var session = optSession.get();
		var optUser = users.load(session.userId());
		if (optUser.isEmpty()) return invalidSessionUser(ex);
		var user = optUser.get();
		sessions.extend(session, user);

		switch (path) {
			case "":
				return sendUserAndCookie(ex, session, user);
			case "add":
				return addUser(ex, user);
			case "list":
				return list(ex, user);
			case "password":
				return updatePassword(ex, user);
			case "permission":
				return editPermission(ex, user, false);
			case "update":
				return updateUser(ex, user);
		}
		return notFound(ex);
	}

	private boolean editPermission(HttpExchange ex, User user, boolean drop) throws IOException {
		if (!user.hasPermission(MANAGE_USERS)) return sendEmptyResponse(HTTP_FORBIDDEN, ex);
		var json = json(ex);
		if (!json.has(USER_ID)) return badRequest(ex, "Missing user_id in request!");
		if (!json.has(PERMISSION)) return badRequest(ex, "Missing permission in request");
		try {
			var permission = Permission.valueOf(json.getString(PERMISSION));
			var userId     = json.getString(USER_ID);
			var optUer     = users.load(userId);
			if (optUer.isEmpty()) return badRequest(ex, "Unknown user id (%s)".formatted(userId));
			user = optUer.get();
			if (drop) {
				user.drop(permission);
			} else {
				user.add(permission);
			}
			users.save(user);
			return sendEmptyResponse(HTTP_OK, ex);
		} catch (IllegalArgumentException iae) {
			return badRequest(ex, iae.getMessage());
		}
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


	private boolean list(HttpExchange ex, User user) throws IOException {
		if (!user.hasPermission(MANAGE_USERS)) return sendEmptyResponse(HTTP_FORBIDDEN, ex);
		var json = new JSONObject();
		users.list().forEach(u -> json.put(u.uuid(), u.map(false)));
		return sendContent(ex, json);
	}

	private boolean login(HttpExchange ex) throws IOException {
		var body = json(ex);

		var username = body.has(USERNAME) ? body.getString(USERNAME) : null;
		var password = body.has(PASSWORD) ? body.getString(PASSWORD) : null;
		var trust    = body.has(TRUST) && body.getBoolean(TRUST);

		Result<User> result = users.login(username, password);
		if (result instanceof Payload<User> user) return sendUserAndCookie(ex, sessions.createSession(user.get(), trust), user.get());
		return sendContent(ex, HTTP_UNAUTHORIZED, result);
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
		var session = sessions.createSession(user, false);
		new SessionToken(session.id(), "/api", session.expiration(), session.trustBrowser()).addTo(ex);
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
		} catch (MessagingException e) {
			throw new RuntimeException(e);
		}
	}

	private boolean sendUserAndCookie(HttpExchange ex, Session session, User user) throws IOException {
		new SessionToken(session.id(), "/api", session.expiration(), session.trustBrowser()).addTo(ex);
		return sendContent(ex, user.map(false));
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

	private boolean updatePassword(HttpExchange ex, User user) throws IOException {
		var json = json(ex);
		var uuid = json.getString(UUID);
		if (!uuid.equals(user.uuid())) {
			return sendEmptyResponse(HTTP_FORBIDDEN, ex);
		}
		var oldPass = json.getString("oldpass");
		if (!users.passwordMatches(oldPass, user)) return badRequest(ex, "wrong password");

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

	private boolean updateUser(HttpExchange ex, User user) throws IOException {
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
