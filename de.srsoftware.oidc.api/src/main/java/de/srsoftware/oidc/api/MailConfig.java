/* © SRSoftware 2024 */
package de.srsoftware.oidc.api;

import static de.srsoftware.oidc.api.Constants.*;

import java.util.Map;
import java.util.Properties;

public interface MailConfig {
	public String	  smtpHost();
	public MailConfig smtpHost(String newValue);

	public int	  smtpPort();
	public MailConfig smtpPort(int newValue);

	public String	  senderAddress();
	public MailConfig senderAddress(String newValue);

	public String	  senderPassword();
	public MailConfig senderPassword(String newValue);

	default boolean startTls() {
		return true;
	}
	MailConfig startTls(boolean newValue);

	default boolean smtpAuth() {
		return true;
	}
	MailConfig smtpAuth(boolean newValue);

	public default Properties props() {
		Properties props = new Properties();
		props.put("mail.smtp.host", smtpHost());
		props.put("mail.smtp.port", smtpPort());
		props.put("mail.smtp.auth", smtpAuth() ? "true" : "false");
		props.put("mail.smtp.starttls.enable", startTls() ? "true" : "false");
		return props;
	}

	default Map<String, Object> map() {
		return Map.of(		      //
		    SMTP_HOST, smtpHost(),	      //
		    SMTP_PORT, smtpPort(),	      //
		    SMTP_AUTH, smtpAuth(),	      //
		    SENDER_ADDRESS, senderAddress(),  //
		    START_TLS, startTls());
	}
}
