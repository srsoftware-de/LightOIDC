/* © SRSoftware 2024 */
package de.srsoftware.http;


import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import java.util.Optional;


public class SessionToken extends Cookie {
	private final String sessionId;

	public SessionToken(String sessionId) {
		super("sessionToken", sessionId + "; Path=/api");
		this.sessionId = sessionId;
	}

	@Override
	public <T extends Cookie> T addTo(Headers headers) {
		headers.add("session", sessionId);
		return (T)this;	 // super.addTo(headers);
	}

	public static Optional<SessionToken> from(HttpExchange ex) {
		return Cookie.of(ex)
		    .stream()
		    .filter(cookie -> cookie.startsWith("sessionToken="))

		    .map(cookie -> cookie.split("=", 2)[1])
		    .map(id -> new SessionToken(id))
		    .findAny();
	}

	public String sessionId() {
		return sessionId;
	}
}