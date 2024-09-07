/* © SRSoftware 2024 */
package de.srsoftware.oidc.datastore.sqlite;

import de.srsoftware.oidc.api.MailConfig;
import jakarta.mail.Authenticator;
import java.sql.Connection;

public class SqliteMailConfig implements MailConfig {
	public SqliteMailConfig(Connection connection) {
	}

	@Override
	public String smtpHost() {
		return "";
	}

	@Override
	public MailConfig smtpHost(String newValue) {
		return null;
	}

	@Override
	public int smtpPort() {
		return 0;
	}

	@Override
	public MailConfig smtpPort(int newValue) {
		return null;
	}

	@Override
	public String senderAddress() {
		return "";
	}

	@Override
	public MailConfig senderAddress(String newValue) {
		return null;
	}

	@Override
	public String senderPassword() {
		return "";
	}

	@Override
	public MailConfig senderPassword(String newValue) {
		return null;
	}

	@Override
	public MailConfig startTls(boolean newValue) {
		return null;
	}

	@Override
	public MailConfig smtpAuth(boolean newValue) {
		return null;
	}

	@Override
	public Authenticator authenticator() {
		return null;
	}

	@Override
	public MailConfig save() {
		return null;
	}
}
