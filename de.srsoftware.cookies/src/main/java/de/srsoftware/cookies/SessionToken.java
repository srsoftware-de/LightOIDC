/* © SRSoftware 2024 */
package de.srsoftware.cookies;


import com.sun.net.httpserver.HttpExchange;
import java.util.Optional;
import java.util.logging.Logger;

import static java.lang.System.Logger.Level.DEBUG;
import static java.lang.System.Logger.Level.WARNING;

public class SessionToken extends Cookie {
	private final String sessionId;

	public SessionToken(String sessionId) {
		super("sessionToken", sessionId+"; Path=/api");
		this.sessionId = sessionId;
	}

	public static Optional<SessionToken> from(HttpExchange ex) {
		return Cookie.of(ex).stream().filter(cookie -> cookie.startsWith("sessionToken="))

				.map(cookie -> cookie.split("=", 2)[1]).map(id -> new SessionToken(id)).findAny();
	}

	public String sessionId() {
		return sessionId;
	}
}