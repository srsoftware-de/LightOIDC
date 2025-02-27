/* © SRSoftware 2024 */
package de.srsoftware.oidc.datastore.sqlite;

import static de.srsoftware.oidc.api.Constants.*;

import de.srsoftware.oidc.api.MailConfig;
import jakarta.mail.Authenticator;
import jakarta.mail.PasswordAuthentication;
import java.sql.Connection;
import java.sql.SQLException;

public class SqliteMailConfig extends SqliteStore implements MailConfig {
	private static final String STORE_VERSION	     = "mail_config_store_version";
	private static final String CREATE_STORE_VERSION     = "INSERT INTO metainfo (key,value) VALUES ('" + STORE_VERSION + "','0')";
	private static final String SELECT_STORE_VERSION     = "SELECT * FROM metainfo WHERE key = '" + STORE_VERSION + "'";
	private static final String SET_STORE_VERSION	     = "UPDATE metainfo SET value = ? WHERE key = '" + STORE_VERSION + "'";
	private static final String CREATE_MAIL_CONFIG_TABLE = "CREATE TABLE mail_config (key VARCHAR(64) PRIMARY KEY, value VARCHAR(255));";
	private static final String SAVE_MAILCONFIG	     = "INSERT INTO mail_config (key, value) VALUES (?, ?) ON CONFLICT DO UPDATE SET value = ?";
	private static final String SELECT_MAILCONFIG	     = "SELECT * FROM mail_config";
	private String	            smtpHost, senderAddress, password;

	private int	      smtpPort;
	private boolean	      smtpAuth, startTls;
	private Authenticator auth;

	public SqliteMailConfig(Connection connection) throws SQLException {
		super(connection);
		smtpHost      = "";
		smtpPort      = 0;
		senderAddress = "";
		password      = "";
		smtpAuth      = true;
		startTls      = true;
		var rs        = conn.prepareStatement(SELECT_MAILCONFIG).executeQuery();
		while (rs.next()) {
			var key = rs.getString(1);
			switch (key) {
				case SMTP_PORT -> smtpPort = rs.getInt(2);
				case SMTP_HOST -> smtpHost = rs.getString(2);
				case START_TLS -> startTls = rs.getBoolean(2);
				case SMTP_AUTH -> smtpAuth = rs.getBoolean(2);
				case SMTP_PASSWORD -> password = rs.getString(2);
				case SMTP_USER -> senderAddress = rs.getString(2);
			}
		}
	}

	private void createStoreTables() throws SQLException {
		conn.prepareStatement(CREATE_MAIL_CONFIG_TABLE).execute();
	}


	@Override
	protected void initTables() throws SQLException {
		var rs	     = conn.prepareStatement(SELECT_STORE_VERSION).executeQuery();
		int availableVersion = 1;
		int currentVersion;
		if (rs.next()) {
			currentVersion = rs.getInt("value");
			rs.close();
		} else {
			rs.close();
			conn.prepareStatement(CREATE_STORE_VERSION).execute();
			currentVersion = 0;
		}

		conn.setAutoCommit(false);
		var stmt = conn.prepareStatement(SET_STORE_VERSION);
		while (currentVersion < availableVersion) {
			try {
				switch (currentVersion) {
					case 0:
						createStoreTables();
						break;
				}
				stmt.setInt(1, ++currentVersion);
				stmt.execute();
				conn.commit();
			} catch (Exception e) {
				conn.rollback();
				LOG.log(System.Logger.Level.ERROR, "Failed to update at {} = {}", STORE_VERSION, currentVersion);
				break;
			}
		}
		conn.setAutoCommit(true);
	}

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
		return smtpPort;
	}

	@Override
	public MailConfig smtpPort(int newValue) {
		smtpPort = newValue;
		return this;
	}

	@Override
	public String senderAddress() {
		return senderAddress;
	}

	@Override
	public MailConfig senderAddress(String newValue) {
		senderAddress = newValue;
		return this;
	}

	@Override
	public String senderPassword() {
		return password;
	}

	@Override
	public MailConfig senderPassword(String newValue) {
		password = newValue;
		return this;
	}

	@Override
	public boolean startTls() {
		return startTls;
	}

	@Override
	public boolean smtpAuth() {
		return smtpAuth;
	}

	@Override
	public MailConfig startTls(boolean newValue) {
		startTls = newValue;
		return this;
	}

	@Override
	public MailConfig smtpAuth(boolean newValue) {
		smtpAuth = newValue;
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
	public MailConfig save() {
		try {
			var stmt = conn.prepareStatement(SAVE_MAILCONFIG);
			for (var entry : map().entrySet()) {
				stmt.setString(1, entry.getKey());
				stmt.setObject(2, entry.getValue());
				stmt.setObject(3, entry.getValue());
				stmt.execute();
			}
			return this;
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}
}
