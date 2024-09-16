/* © SRSoftware 2024 */
package de.srsoftware.oidc.api;

import static de.srsoftware.oidc.api.Constants.*;
import static de.srsoftware.utils.Strings.uuid;
import static org.junit.jupiter.api.Assertions.*;

import jakarta.mail.Authenticator;
import jakarta.mail.PasswordAuthentication;
import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.util.Map;
import java.util.Random;
import org.junit.jupiter.api.Test;

public abstract class MailConfigTest {
	protected abstract MailConfig mailConfig();
	protected abstract void       reOpen();

	@Test
	public void testSmtpHost() {
		assertEquals("", mailConfig().smtpHost());
		var host = uuid();
		mailConfig().smtpHost(host);
		assertEquals(host, mailConfig().smtpHost());
	}

	@Test
	public void testSmtpPort() {
		assertEquals(0, mailConfig().smtpPort());
		var port = new Random().nextInt();
		mailConfig().smtpPort(port);
		assertEquals(port, mailConfig().smtpPort());
	}

	@Test
	public void testSenderAddress() {
		assertEquals("", mailConfig().senderAddress());
		var address = uuid();
		mailConfig().senderAddress(address);
		assertEquals(address, mailConfig().senderAddress());
	}

	@Test
	public void testSenderPassword() {
		assertEquals("", mailConfig().senderPassword());
		var password = uuid();
		mailConfig().senderPassword(password);
		assertEquals(password, mailConfig().senderPassword());
	}

	@Test
	public void testStartTls() {
		mailConfig().startTls(false);
		assertFalse(mailConfig().startTls());
		mailConfig().startTls(true);
		assertTrue(mailConfig().startTls());
		mailConfig().startTls(false);
		assertFalse(mailConfig().startTls());
	}

	@Test
	public void testSmtpAuth() {
		mailConfig().smtpAuth(false);
		assertFalse(mailConfig().smtpAuth());
		mailConfig().smtpAuth(true);
		assertTrue(mailConfig().smtpAuth());
		mailConfig().smtpAuth(false);
		assertFalse(mailConfig().smtpAuth());
	}

	@Test
	public void testProps() {
		var host     = uuid();
		var port     = new Random().nextInt();
		var address  = uuid();
		var password = uuid();
		mailConfig().senderPassword(password);
		mailConfig().senderAddress(address);
		mailConfig().smtpHost(host);
		mailConfig().smtpPort(port);
		mailConfig().startTls(true);
		mailConfig().smtpAuth(true);

		var props = mailConfig().props();
		assertEquals(host, props.get("mail.smtp.host"));
		assertEquals(port, props.get("mail.smtp.port"));
		assertEquals(host, props.get("mail.smtp.ssl.trust"));
		assertEquals("true", props.get("mail.smtp.auth"));
		assertEquals("true", props.get("mail.smtp.starttls.enable"));

		mailConfig().startTls(false);
		mailConfig().smtpAuth(false);
		props = mailConfig().props();
		assertEquals(host, props.get("mail.smtp.host"));
		assertEquals(port, props.get("mail.smtp.port"));
		assertEquals(host, props.get("mail.smtp.ssl.trust"));
		assertEquals("false", props.get("mail.smtp.auth"));
		assertEquals("false", props.get("mail.smtp.starttls.enable"));
	}

	@Test
	public void testMap() {
		var host     = uuid();
		var port     = new Random().nextInt();
		var address  = uuid();
		var password = uuid();

		mailConfig().senderPassword(password);
		mailConfig().senderAddress(address);
		mailConfig().smtpHost(host);
		mailConfig().smtpPort(port);
		mailConfig().startTls(true);
		mailConfig().smtpAuth(false);
		var map = mailConfig().map();
		assertEquals(map, Map.of(	           //
			      SMTP_HOST, host,     //
			      SMTP_PORT, port,     //
			      SMTP_AUTH, false,    //
			      SMTP_USER, address,  //
			      START_TLS, true));

		mailConfig().startTls(false);
		mailConfig().smtpAuth(true);
		map = mailConfig().map();
		assertEquals(map, Map.of(	           //
			      SMTP_HOST, host,     //
			      SMTP_PORT, port,     //
			      SMTP_AUTH, true,     //
			      SMTP_USER, address,  //
			      START_TLS, false));
	}

	@Test
	public void testAuthenticator() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
		var host     = uuid();
		var port     = new Random().nextInt();
		var address  = uuid();
		var password = uuid();

		mailConfig().senderPassword(password);
		mailConfig().senderAddress(address);
		mailConfig().smtpHost(host);
		mailConfig().smtpPort(port);
		mailConfig().startTls(true);
		mailConfig().smtpAuth(false);
		Authenticator authenticator = mailConfig().authenticator();
		var           method        = authenticator.getClass().getDeclaredMethod("getPasswordAuthentication");
		method.setAccessible(true);
		var o = method.invoke(authenticator);
		assertTrue(o instanceof PasswordAuthentication);
		var pwa = (PasswordAuthentication)o;
		assertEquals(password, pwa.getPassword());
		assertEquals(address, pwa.getUserName());
	}

	@Test
	public void testSave() throws SQLException {
		var host     = uuid();
		var port     = new Random().nextInt();
		var address  = uuid();
		var password = uuid();

		mailConfig().senderPassword(password);
		mailConfig().senderAddress(address);
		mailConfig().smtpHost(host);
		mailConfig().smtpPort(port);
		mailConfig().startTls(true);
		mailConfig().smtpAuth(false);
		mailConfig().save();
		reOpen();

		var map = mailConfig().map();
		assertEquals(map, Map.of(	           //
			      SMTP_HOST, host,     //
			      SMTP_PORT, port,     //
			      SMTP_AUTH, false,    //
			      SMTP_USER, address,  //
			      START_TLS, true));

		mailConfig().startTls(false);
		mailConfig().smtpAuth(true);
		mailConfig().save();
		reOpen();

		map = mailConfig().map();
		assertEquals(map, Map.of(	           //
			      SMTP_HOST, host,     //
			      SMTP_PORT, port,     //
			      SMTP_AUTH, true,     //
			      SMTP_USER, address,  //
			      START_TLS, false));
	}
}
