/* © SRSoftware 2024 */
package de.srsoftware.oidc.api;


public class SessionToken extends Cookie {
	public SessionToken(String value) {
		super("sessionToken", value);
	}
}