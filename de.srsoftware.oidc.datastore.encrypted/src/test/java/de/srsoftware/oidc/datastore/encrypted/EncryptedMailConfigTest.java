/* © SRSoftware 2025 */
package de.srsoftware.oidc.datastore.encrypted; /* © SRSoftware 2024 */
import static de.srsoftware.tools.Strings.uuid;
import static org.junit.jupiter.api.Assertions.*;

import de.srsoftware.oidc.api.MailConfig;
import jakarta.mail.Authenticator;
import java.util.Map;
import java.util.Properties;
import org.junit.jupiter.api.Test;

public class EncryptedMailConfigTest {
	private class InMemoryMailConfig implements MailConfig {
		private String	smtpHost;
		private int	port;
		private String	addr;
		private String	pass;
		private boolean tls;
		private boolean auth;

		@Override
		public String smtpHost() {
			return smtpHost;
		}

		@Override
		public MailConfig smtpHost(String newValue) {
			smtpHost = newValue;
			return this;
		}

		@Override
		public int smtpPort() {
			return port;
		}

		@Override
		public MailConfig smtpPort(int newValue) {
			port = newValue;
			return this;
		}

		@Override
		public String senderAddress() {
			return addr;
		}

		@Override
		public MailConfig senderAddress(String newValue) {
			addr = newValue;
			return this;
		}

		@Override
		public String senderPassword() {
			return pass;
		}

		@Override
		public MailConfig senderPassword(String newValue) {
			pass = newValue;
			return this;
		}

		@Override
		public boolean startTls() {
			return tls;
		}

		@Override
		public MailConfig startTls(boolean newValue) {
			tls = newValue;
			return this;
		}

		@Override
		public boolean smtpAuth() {
			return auth;
		}

		@Override
		public MailConfig smtpAuth(boolean newValue) {
			auth = newValue;
			return this;
		}

		@Override
		public Properties props() {
			return null;
		}

		@Override
		public Map<String, Object> map() {
			return null;
		}

		@Override
		public Authenticator authenticator() {
			return null;
		}

		@Override
		public MailConfig save() {
			return this;
		}
	}
	@Test
	public void TestEncryptedMailConfig() {
		var key  = uuid();
		var salt = uuid();


		var addr = uuid();
		var pass = uuid();
		var host = uuid();

		var plainMailConfig = new InMemoryMailConfig();
		var writer	    = new EncryptedMailConfig(plainMailConfig, key, salt);

		writer.senderAddress(addr).senderPassword(pass).smtpHost(host).smtpAuth(true).startTls(false);

		var reader = new EncryptedMailConfig(plainMailConfig, key, salt);
		assertEquals(addr, reader.senderAddress());
		assertEquals(host, reader.smtpHost());
		assertEquals(pass, reader.senderPassword());
		assertTrue(reader.smtpAuth());
		assertFalse(reader.startTls());
	}
}
