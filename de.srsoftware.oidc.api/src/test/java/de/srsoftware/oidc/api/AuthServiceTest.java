/* © SRSoftware 2024 */
package de.srsoftware.oidc.api;

import static de.srsoftware.oidc.api.Constants.OPENID;
import static de.srsoftware.utils.Strings.uuid;
import static java.time.temporal.ChronoUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.Test;

public abstract class AuthServiceTest {
	private static final String CLIENT1       = "client1";
	private static final Set<String> SCOPES1  = Set.of(OPENID, Constants.EMAIL, "ranzpappe");
	private static final String	 INVALID  = "invalid";
	private static final String	 PASS1    = "grunzwanzling";
	private static final String	 USERNAME = "arthurdent";
	private static final String	 REALNAME = "Arthur Dent";
	private static final String	 EMAIL    = "arthur@herzaus.gold";

	protected abstract AuthorizationService authorizationService();

	@Test
	public void testAuthorize() {
		var authorizationService = authorizationService();
		var userId1	         = uuid();
		var expiration	         = Instant.now();
		authorizationService.authorize(userId1, CLIENT1, SCOPES1, expiration);
		expiration = Instant.now().plusSeconds(3600).truncatedTo(SECONDS);      // test overwrite
		authorizationService.authorize(userId1, CLIENT1, SCOPES1, expiration);  // test overwrite
		var authorization = authorizationService.getAuthorization(userId1, CLIENT1, Set.of(OPENID));
		assertEquals(1, authorization.authorizedScopes().scopes().size());
		assertTrue(authorization.authorizedScopes().scopes().contains(OPENID));
		assertEquals(expiration, authorization.authorizedScopes().expiration());

		authorization = authorizationService.getAuthorization(userId1, CLIENT1, Set.of(INVALID));
		assertNull(authorization.authorizedScopes());
		assertNull(authorization.authCode());
		assertTrue(authorization.unauthorizedScopes().contains(INVALID));

		authorization = authorizationService.getAuthorization(userId1, CLIENT1, Set.of(INVALID, OPENID));
		assertEquals(1, authorization.authorizedScopes().scopes().size());
		assertTrue(authorization.authorizedScopes().scopes().contains(OPENID));
		assertEquals(expiration, authorization.authorizedScopes().expiration());

		assertEquals(1, authorization.unauthorizedScopes().size());
		assertTrue(authorization.unauthorizedScopes().contains(INVALID));
	}

	@Test
	public void testConsume() {
		var authorizationService = authorizationService();

		var userId1    = uuid();
		var expiration = Instant.now().plusSeconds(3600).truncatedTo(SECONDS);
		authorizationService.authorize(userId1, CLIENT1, SCOPES1, expiration);
		var authResult = authorizationService.getAuthorization(userId1, CLIENT1, Set.of(OPENID));
		var authCode   = authResult.authCode();
		assertNotNull(authCode);

		var optAuth = authorizationService.consumeAuthorization(authCode);
		assertTrue(optAuth.isPresent());
		var authorization = optAuth.get();
		assertEquals(CLIENT1, authorization.clientId());
		assertEquals(userId1, authorization.userId());
		var scopes = authorization.scopes();
		assertEquals(expiration, scopes.expiration());
		assertEquals(1, scopes.scopes().size());
		assertTrue(scopes.scopes().contains(OPENID));

		optAuth = authorizationService.consumeAuthorization(authCode);
		assertTrue(optAuth.isEmpty());
	}

	// TODO: test nonce methods
	// TODO: test authorizedClients method
}
