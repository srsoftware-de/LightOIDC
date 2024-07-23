/* © SRSoftware 2024 */
package de.srsoftware.cookies;


import com.sun.net.httpserver.HttpExchange;
import java.util.Optional;

public class SessionToken extends Cookie {
	private final String sessionId;

	public SessionToken(String sessionId) {
		super("sessionToken", sessionId);
		this.sessionId = sessionId;
	}

	public static Optional<SessionToken> from(HttpExchange ex) {
		return Cookie.of(ex).stream().filter(cookie -> cookie.startsWith("sessionToken=")).map(cookie -> cookie.split("=", 2)[1]).map(id -> new SessionToken(id)).findAny();
	}

	public String sessionId() {
		return sessionId;
	}
}