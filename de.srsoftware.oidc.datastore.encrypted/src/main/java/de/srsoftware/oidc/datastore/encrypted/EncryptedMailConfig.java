/* © SRSoftware 2024 */
package de.srsoftware.oidc.datastore.encrypted;

/* © SRSoftware 2024 */

import static de.srsoftware.oidc.api.Constants.*;

import de.srsoftware.oidc.api.MailConfig;
import jakarta.mail.Authenticator;
import jakarta.mail.PasswordAuthentication;


public class EncryptedMailConfig extends EncryptedConfig implements MailConfig {
	private final MailConfig storage;
	private Authenticator    auth;

	public EncryptedMailConfig(MailConfig storage, String encryptionKey, String salt) {
		super(encryptionKey, salt);
		this.storage = storage;
	}

	@Override
	public MailConfig save() {
		return storage.save();
	}

	@Override
	public String senderAddress() {
		return decrypt(storage.senderAddress());
	}

	@Override
	public MailConfig senderAddress(String newValue) {
		storage.senderAddress(encrypt(newValue));
		return this;
	}

	@Override
	public String senderPassword() {
		return decrypt(storage.senderPassword());
	}

	@Override
	public MailConfig senderPassword(String newValue) {
		storage.senderPassword(encrypt(newValue));
		return this;
	}

	@Override
	public boolean smtpAuth() {
		return storage.smtpAuth();
	}

	@Override
	public MailConfig smtpAuth(boolean newValue) {
		storage.smtpAuth(newValue);
		return this;
	}

	@Override
	public Authenticator authenticator() {
		if (auth == null) {
			auth = new Authenticator() {
				// override the getPasswordAuthentication method
				protected PasswordAuthentication getPasswordAuthentication() {
					return new PasswordAuthentication(senderAddress(), senderPassword());
				}
			};
		}
		return auth;
	}

	@Override
	public String smtpHost() {
		return decrypt(storage.smtpHost());
	}

	@Override
	public MailConfig smtpHost(String newValue) {
		storage.smtpHost(encrypt(newValue));
		return this;
	}

	@Override
	public int smtpPort() {
		return storage.smtpPort();
	}

	@Override
	public MailConfig smtpPort(int newValue) {
		storage.smtpPort(newValue);
		return this;
	}

	@Override
	public boolean startTls() {
		return storage.startTls();
	}

	@Override
	public MailConfig startTls(boolean newValue) {
		storage.startTls(newValue);
		return this;
	}
}
