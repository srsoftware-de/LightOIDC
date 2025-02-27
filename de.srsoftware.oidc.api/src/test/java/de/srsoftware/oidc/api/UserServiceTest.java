/* © SRSoftware 2024 */
package de.srsoftware.oidc.api;

import static de.srsoftware.oidc.api.data.Permission.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.srsoftware.oidc.api.data.Permission;
import de.srsoftware.oidc.api.data.User;
import de.srsoftware.tools.PasswordHasher;
import de.srsoftware.tools.UuidHasher;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public abstract class UserServiceTest {
	private static final String EMAIL     = "heinz@ellmann.de";
	private static final String EMAIL2    = "arno@nym.de";
	private static final String NAME      = "Heinz Ellmann";
	private static final String NAME2     = "Arno Nym";
	private static final String PASSWORD  = "absolutelysafe";
	private static final String PASSWORD2 = "evenbetterpassword";
	private static final String USERNAME  = "heinz_ellmann";
	private static final String USERNAME2 = "arno_nym";

	protected abstract UserService userService();

	private PasswordHasher<String> hasher = null;

	protected PasswordHasher<String> hasher() {
		if (hasher == null) try {
				hasher = new UuidHasher();
			} catch (NoSuchAlgorithmException e) {
				throw new RuntimeException(e);
			}

		return hasher;
	}


	@Test
	public void testListEmpty() {
		var users = userService().list();
		Assertions.assertEquals(0, users.size());
	}

	@Test
	public void testInit() {
		var uuid       = UUID.randomUUID().toString();
		var hashedPass = hasher().hash(PASSWORD, uuid);
		var firstUser  = new User(USERNAME, hashedPass, NAME, EMAIL, uuid);
		userService().init(firstUser);
		var users = userService().list();
		Assertions.assertEquals(1, users.size());
		var saved = users.get(0);
		assertTrue(hasher().matches(PASSWORD, saved.hashedPassword()));
		Assertions.assertEquals(firstUser, saved);
	}

	@Test
	public void testSave() {
		var uuid       = UUID.randomUUID().toString();
		var hashedPass = hasher().hash(PASSWORD, uuid);
		var newUser    = new User(USERNAME, hashedPass, NAME, EMAIL, uuid);
		newUser.add(MANAGE_CLIENTS);
		newUser.add(MANAGE_PERMISSIONS);
		userService().save(newUser);
		var users = userService().list();
		Assertions.assertEquals(1, users.size());
		var saved = users.get(0);
		assertTrue(hasher().matches(PASSWORD, saved.hashedPassword()));
		Assertions.assertEquals(newUser, saved);
		Assertions.assertFalse(saved.hasPermission(Permission.MANAGE_USERS));
		Assertions.assertFalse(saved.hasPermission(Permission.MANAGE_SMTP));
		assertTrue(saved.hasPermission(MANAGE_CLIENTS));
		assertTrue(saved.hasPermission(MANAGE_PERMISSIONS));
	}

	@Test
	public void testLoad() {
		var uuid       = UUID.randomUUID().toString();
		var hashedPass = hasher().hash(PASSWORD, uuid);
		var newUser    = new User(USERNAME, hashedPass, NAME, EMAIL, uuid);
		newUser.add(MANAGE_CLIENTS);
		newUser.add(MANAGE_PERMISSIONS);
		userService().save(newUser);
		var saved = userService().load(uuid);
		assertTrue(saved.isPresent());
		Assertions.assertEquals(newUser, saved.get());
	}

	@Test
	public void testFind() {
		var uuid1 = UUID.randomUUID().toString();
		var pass1 = hasher().hash(PASSWORD, uuid1);
		var user1 = new User("hicke", pass1, "Heiko Icke", "h.icke@example.com", uuid1);

		var uuid2 = UUID.randomUUID().toString();
		var pass2 = hasher().hash(PASSWORD, uuid2);
		var user2 = new User("franz", pass2, "hicke", "franz@example.com", uuid2);

		var uuid3 = UUID.randomUUID().toString();
		var pass3 = hasher().hash(PASSWORD, uuid3);
		var user3 = new User("jutta", pass3, "Jutta", "hicke", uuid3);

		var uuid4 = UUID.randomUUID().toString();
		var pass4 = hasher().hash(PASSWORD, uuid4);
		var user4 = new User("annabolika", pass4, "Anna Bolika", "anna@example.com", uuid4);

		userService().save(user1).save(user2).save(user3).save(user4);
		Assertions.assertEquals(4, userService().list().size());
		var found = userService().find("hicke");
		Assertions.assertEquals(3, found.size());

		Assertions.assertEquals(1, userService().find("Anna Bolika").size());
		Assertions.assertEquals(0, userService().find("nosferatu").size());
	}

	@Test
	public void testAlterPassword() {
		var uuid       = UUID.randomUUID().toString();
		var hashedPass = hasher().hash(PASSWORD, uuid);
		var firstUser  = new User(USERNAME, hashedPass, NAME, EMAIL, uuid);
		userService().init(firstUser);

		var loaded = userService().load(uuid);
		assertTrue(loaded.isPresent());
		var oldPass = loaded.get().hashedPassword();
		assertTrue(hasher().matches(PASSWORD, oldPass));

		var newPass = hasher().hash(PASSWORD2, uuid);
		userService().save(firstUser.hashedPassword(newPass));

		loaded = userService().load(uuid);
		assertTrue(loaded.isPresent());
		newPass = loaded.get().hashedPassword();
		assertTrue(hasher().matches(PASSWORD2, newPass));

		userService().updatePassword(firstUser, PASSWORD);
		loaded = userService().load(uuid);
		assertTrue(loaded.isPresent());
		assertTrue(userService().passwordMatches(PASSWORD, loaded.get()));
	}

	@Test
	public void testAlterUsername() {
		var uuid       = UUID.randomUUID().toString();
		var hashedPass = hasher().hash(PASSWORD, uuid);
		var firstUser  = new User(USERNAME, hashedPass, NAME, EMAIL, uuid);
		userService().init(firstUser);

		var loaded = userService().load(uuid);
		assertTrue(loaded.isPresent());
		Assertions.assertEquals(USERNAME, loaded.get().username());

		userService().save(firstUser.username(USERNAME2));

		loaded = userService().load(uuid);
		assertTrue(loaded.isPresent());
		Assertions.assertEquals(USERNAME2, loaded.get().username());
	}

	@Test
	public void testAlterRealname() {
		var uuid       = UUID.randomUUID().toString();
		var hashedPass = hasher().hash(PASSWORD, uuid);
		var firstUser  = new User(USERNAME, hashedPass, NAME, EMAIL, uuid);
		userService().init(firstUser);

		var loaded = userService().load(uuid);
		assertTrue(loaded.isPresent());
		Assertions.assertEquals(NAME, loaded.get().realName());

		userService().save(firstUser.realName(NAME2));

		loaded = userService().load(uuid);
		assertTrue(loaded.isPresent());
		Assertions.assertEquals(NAME2, loaded.get().realName());
	}

	@Test
	public void testAlterEmail() {
		var uuid       = UUID.randomUUID().toString();
		var hashedPass = hasher().hash(PASSWORD, uuid);
		var firstUser  = new User(USERNAME, hashedPass, NAME, EMAIL, uuid);
		userService().init(firstUser);

		var loaded = userService().load(uuid);
		assertTrue(loaded.isPresent());
		Assertions.assertEquals(NAME, loaded.get().realName());

		userService().save(firstUser.email(EMAIL2));

		loaded = userService().load(uuid);
		assertTrue(loaded.isPresent());
		Assertions.assertEquals(EMAIL2, loaded.get().email());
	}

	@Test
	public void testAlterPermissions() {
		var uuid       = UUID.randomUUID().toString();
		var hashedPass = hasher().hash(PASSWORD, uuid);
		var firstUser  = new User(USERNAME, hashedPass, NAME, EMAIL, uuid);
		userService().init(firstUser);

		var opt = userService().load(uuid);
		assertTrue(opt.isPresent());
		var loaded = opt.get();
		for (var permission : Permission.values()) Assertions.assertFalse(loaded.hasPermission(permission));

		userService().save(loaded.add(MANAGE_CLIENTS, MANAGE_PERMISSIONS));

		opt = userService().load(uuid);
		assertTrue(opt.isPresent());
		loaded = opt.get();
		assertTrue(loaded.hasPermission(MANAGE_CLIENTS));
		assertTrue(loaded.hasPermission(MANAGE_PERMISSIONS));
		Assertions.assertFalse(loaded.hasPermission(MANAGE_SMTP));
		Assertions.assertFalse(loaded.hasPermission(MANAGE_USERS));

		userService().save(loaded.add(MANAGE_SMTP, MANAGE_USERS).drop(MANAGE_CLIENTS, MANAGE_PERMISSIONS));
		opt = userService().load(uuid);
		assertTrue(opt.isPresent());
		loaded = opt.get();
		Assertions.assertFalse(loaded.hasPermission(MANAGE_CLIENTS));
		Assertions.assertFalse(loaded.hasPermission(MANAGE_PERMISSIONS));
		assertTrue(loaded.hasPermission(MANAGE_SMTP));
		assertTrue(loaded.hasPermission(MANAGE_USERS));
	}
}
