/* © SRSoftware 2024 */
package de.srsoftware.oidc.api;

import static de.srsoftware.oidc.api.Constants.*;
import static java.lang.System.Logger.Level.WARNING;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

public record Client(String id, String name, String secret, Set<String> redirectUris) {
	private static System.Logger LOG = System.getLogger(Client.class.getSimpleName());
	public Map<String, Object> map() {
		return Map.of(CLIENT_ID, id, NAME, name, SECRET, secret, REDIRECT_URIS, redirectUris);
	}

	public String generateCode() {
		LOG.log(WARNING,"{0}.generateCode() not implemented!", getClass().getSimpleName());
		return UUID.randomUUID().toString();
	}
}
