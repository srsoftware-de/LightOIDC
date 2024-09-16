/* © SRSoftware 2024 */
package de.srsoftware.oidc.api.data;


import static de.srsoftware.oidc.api.Constants.*;

import java.util.*;

public final class Client {
	private static System.Logger LOG = System.getLogger(Client.class.getSimpleName());
	private final String         id, name, secret;
	private final Set<String> redirectUris;

	public Client(String id, String name, String secret, Set<String> redirectUris) {
		this.id	  = id;
		this.name	  = name;
		this.secret	  = secret;
		this.redirectUris = redirectUris;
	}

	public String id() {
		return id;
	}

	public Map<String, Object> map() {
		return Map.of(CLIENT_ID, id, NAME, name, SECRET, secret, REDIRECT_URIS, redirectUris);
	}


	public String name() {
		return name;
	}

	public String secret() {
		return secret;
	}


	public Set<String> redirectUris() {
		return redirectUris;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) return true;
		if (obj == null || obj.getClass() != this.getClass()) return false;
		var that = (Client)obj;
		return Objects.equals(this.id, that.id) && Objects.equals(this.name, that.name) && Objects.equals(this.secret, that.secret) && Objects.equals(this.redirectUris, that.redirectUris);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, name, secret, redirectUris);
	}

	@Override
	public String toString() {
		return "Client["
		    + "id=" + id + ", "
		    + "name=" + name + ", "
		    + "secret=" + secret + ", "
		    + "redirectUris=" + redirectUris + ']';
	}
}
