/* © SRSoftware 2024 */
package de.srsoftware.http;


import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Optional;


public class SessionToken extends Cookie {
	private final String	       sessionId;
	private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss O");

	public SessionToken(String sessionId, Instant expiration, boolean trust) {
		super("sessionToken", sessionToken(sessionId, expiration, trust));
		this.sessionId = sessionId;
	}

	private static String sessionToken(String sessionId, Instant expiration, boolean trust) {
		if (trust) return "%s; Path=/api; Expires=%s".formatted(sessionId, FORMAT.format(expiration.atZone(ZoneOffset.UTC)));
		return "%s; Path=/api".formatted(sessionId);
	}

	public SessionToken(String sessionId) {
		super("sessionToken", sessionId + "; Path=/api");
		this.sessionId = sessionId;
	}

	@Override
	public <T extends Cookie> T addTo(Headers headers) {
		headers.add("session", getValue());
		return super.addTo(headers);
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