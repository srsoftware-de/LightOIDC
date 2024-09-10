/* © SRSoftware 2024 */
package de.srsoftware.oidc.backend;

import static de.srsoftware.oidc.api.Constants.*;
import static de.srsoftware.oidc.api.data.Permission.MANAGE_SMTP;
import static java.net.HttpURLConnection.HTTP_FORBIDDEN;
import static java.net.HttpURLConnection.HTTP_UNAUTHORIZED;

import com.sun.net.httpserver.HttpExchange;
import de.srsoftware.oidc.api.MailConfig;
import de.srsoftware.oidc.api.SessionService;
import de.srsoftware.oidc.api.UserService;
import de.srsoftware.oidc.api.data.Session;
import java.io.IOException;

public class EmailController extends Controller {
	private final MailConfig  mailConfig;
	private final UserService users;

	public EmailController(MailConfig mailConfig, SessionService sessionService, UserService userService) {
		super(sessionService);
		this.mailConfig = mailConfig;
		users	= userService;
	}

	@Override
	public boolean doGet(String path, HttpExchange ex) throws IOException {
		var optSession = getSession(ex);
		if (optSession.isEmpty()) return sendEmptyResponse(HTTP_UNAUTHORIZED, ex);
		var session = optSession.get();
		var optUser = users.load(session.userId());
		if (optUser.isEmpty()) return invalidSessionUser(ex);
		var user = optUser.get();
		sessions.extend(session, user);

		switch (path) {
			case "/settings":
				return provideSettings(ex, session);
		}
		return notFound(ex);
	}

	@Override
	public boolean doPost(String path, HttpExchange ex) throws IOException {
		var optSession = getSession(ex);
		if (optSession.isEmpty()) return sendEmptyResponse(HTTP_UNAUTHORIZED, ex);
		var session = optSession.get();
		var optUser = users.load(session.userId());
		if (optUser.isEmpty()) return invalidSessionUser(ex);
		var user = optUser.get();
		sessions.extend(session, user);

		switch (path) {
			case "/settings":
				return saveSettings(ex, session);
		}
		return notFound(ex);
	}

	private boolean provideSettings(HttpExchange ex, Session session) throws IOException {
		var optUser = users.load(session.userId());
		if (optUser.isEmpty()) return invalidSessionUser(ex);
		if (!optUser.get().hasPermission(MANAGE_SMTP)) return sendEmptyResponse(HTTP_FORBIDDEN, ex);
		return sendContent(ex, mailConfig.map());
	}

	private boolean saveSettings(HttpExchange ex, Session session) throws IOException {
		var optUser = users.load(session.userId());
		if (optUser.isEmpty()) return invalidSessionUser(ex);
		if (!optUser.get().hasPermission(MANAGE_SMTP)) return sendEmptyResponse(HTTP_FORBIDDEN, ex);
		var data = json(ex);
		if (data.has(SMTP_HOST)) mailConfig.smtpHost(data.getString(SMTP_HOST));
		if (data.has(SMTP_PORT)) mailConfig.smtpPort(data.getInt(SMTP_PORT));
		if (data.has(SMTP_USER)) mailConfig.senderAddress(data.getString(SMTP_USER));
		if (data.has(SMTP_PASSWORD)) mailConfig.senderPassword(data.getString(SMTP_PASSWORD));
		if (data.has(SMTP_AUTH)) mailConfig.smtpAuth(data.getBoolean(SMTP_AUTH));
		if (data.has(START_TLS)) mailConfig.startTls(data.getBoolean(START_TLS));
		mailConfig.save();
		return sendContent(ex, "saved");
	}
}
