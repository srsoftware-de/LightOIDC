/* © SRSoftware 2025 */
package de.srsoftware.oidc.api;

import static de.srsoftware.tools.Strings.uuid;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.srsoftware.oidc.api.data.User;
import de.srsoftware.tools.PasswordHasher;
import de.srsoftware.tools.UuidHasher;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;

public abstract class SessionServiceTest {
	private PasswordHasher<String> hasher = null;

	private static final String EMAIL    = "arno@nym.de";
	private static final String PASSWORD = "grunzwanzling";
	private static final String REALNAME = "Arno Nym";
	private static final String USERNAME = "arno";

	protected PasswordHasher<String> hasher() {
		if (hasher == null) try {
				hasher = new UuidHasher();
			} catch (NoSuchAlgorithmException e) {
				throw new RuntimeException(e);
			}

		return hasher;
	}

	protected abstract SessionService sessionService();

	@Test
	public void testCreateAndLoad() {
		var uuid = uuid();
		var pass = hasher().hash(PASSWORD, uuid);
		var user = new User(USERNAME, pass, REALNAME, EMAIL, uuid).sessionDuration(Duration.ofMinutes(5));

		Instant now	   = Instant.now();
		var     session	   = sessionService().createSession(user, false);
		var     expiration = session.expiration();
		assertTrue(expiration.isAfter(now.plus(5, ChronoUnit.MINUTES).minusSeconds(1)));
		assertTrue(expiration.isBefore(now.plus(5, ChronoUnit.MINUTES).plusSeconds(1)));

		var loaded = sessionService().retrieve(session.id());
		assertTrue(loaded.isPresent());
		assertEquals(session, loaded.get());
	}

	@Test
	public void testCreateAndExtend() {
		var uuid = uuid();
		var pass = hasher().hash(PASSWORD, uuid);
		var user = new User(USERNAME, pass, REALNAME, EMAIL, uuid).sessionDuration(Duration.ofMinutes(5));

		var session = sessionService().createSession(user, false);

		Instant now = Instant.now();
		sessionService().extend(session, user.sessionDuration(Duration.ofMinutes(10)));
		var loaded = sessionService().retrieve(session.id());
		assertTrue(loaded.isPresent());
		assertEquals(session.id(), loaded.get().id());
		var expiration = loaded.get().expiration();
		assertTrue(expiration.isAfter(now.plus(10, ChronoUnit.MINUTES).minusSeconds(1)));
		assertTrue(expiration.isBefore(now.plus(10, ChronoUnit.MINUTES).plusSeconds(1)));
	}

	@Test
	public void textCreateAndDrop() {
		var uuid = uuid();
		var pass = hasher().hash(PASSWORD, uuid);
		var user = new User(USERNAME, pass, REALNAME, EMAIL, uuid).sessionDuration(Duration.ofMinutes(5));

		var session = sessionService().createSession(user, false);
		assertTrue(sessionService().retrieve(session.id()).isPresent());

		sessionService().dropSession(session.id());
		var loaded = sessionService().retrieve(session.id());
		assertTrue(loaded.isEmpty());
	}

	@Test
	public void testExpiration() throws InterruptedException {
		var uuid = uuid();
		var pass = hasher().hash(PASSWORD, uuid);
		var user = new User(USERNAME, pass, REALNAME, EMAIL, uuid).sessionDuration(Duration.ofSeconds(2));

		var session = sessionService().createSession(user, false);
		assertTrue(sessionService().retrieve(session.id()).isPresent());

		Thread.sleep(2500);

		assertTrue(sessionService().retrieve(session.id()).isEmpty());
	}
}
