/* © SRSoftware 2024 */
package de.srsoftware.oidc.datastore.file;

import static de.srsoftware.utils.Strings.uuid;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.srsoftware.oidc.api.SessionService;
import de.srsoftware.oidc.api.data.User;
import de.srsoftware.utils.PasswordHasher;
import de.srsoftware.utils.UuidHasher;
import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class SessionServiceTest {
	private PasswordHasher<String> hasher  = null;
	private File	               storage = new File("/tmp/" + UUID.randomUUID());
	private SessionService         sessionService;

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

	@BeforeEach
	public void setup() throws IOException {
		if (storage.exists()) storage.delete();
		sessionService = new FileStore(storage, hasher());
	}

	@Test
	public void testCreate() {
		var uuid = uuid();
		var pass = hasher().hash(PASSWORD, uuid);
		var user = new User(USERNAME, pass, REALNAME, EMAIL, uuid).sessionDuration(Duration.ofMinutes(5));

		Instant now	   = Instant.now();
		var     session	   = sessionService.createSession(user);
		var     expiration = session.expiration();
		assertTrue(expiration.isAfter(now.plus(5, ChronoUnit.MINUTES).minusSeconds(1)));
		assertTrue(expiration.isBefore(now.plus(5, ChronoUnit.MINUTES).plusSeconds(1)));
	}
}
