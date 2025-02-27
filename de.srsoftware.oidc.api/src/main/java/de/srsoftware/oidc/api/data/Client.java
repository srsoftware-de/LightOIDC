/* © SRSoftware 2024 */
package de.srsoftware.oidc.api.data;


import static de.srsoftware.oidc.api.Constants.*;
import static de.srsoftware.tools.Optionals.nullable;

import java.time.Duration;
import java.util.*;

public final class Client {
	private static System.Logger LOG = System.getLogger(Client.class.getSimpleName());
	private final String         id;
	private String	             landingPage;
	private final String         name;
	private final String         secret;
	private final Set<String> redirectUris;
	private Duration          tokenValidity;

	public Client(String id, String name, String secret, Set<String> redirectUris) {
		this.id	   = id;
		landingPage	   = null;
		this.name	   = name;
		this.secret	   = secret;
		this.redirectUris  = redirectUris;
		this.tokenValidity = Duration.ofMinutes(10);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) return true;
		if (obj == null || obj.getClass() != this.getClass()) return false;
		var that = (Client)obj;
		return Objects.equals(this.id, that.id)	             //
		    && Objects.equals(this.name, that.name)	             //
		    && Objects.equals(this.secret, that.secret)	             //
		    && Objects.equals(this.redirectUris, that.redirectUris)  //
		    && Objects.equals(landingPage, that.landingPage)         //
		    && Objects.equals(tokenValidity, that.tokenValidity);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, name, secret, redirectUris);
	}


	public String id() {
		return id;
	}

	public String landingPage() {
		return landingPage;
	}

	public Client landingPage(String newVal) {
		landingPage = newVal;
		return this;
	}

	public Map<String, Object> map() {
		var map = safeMap();
		map.put(SECRET, secret);
		return map;
	}


	public String name() {
		return name;
	}

	public Map<String, Object> safeMap() {
		var map = new HashMap<String, Object>();
		map.put(CLIENT_ID, id);
		map.put(NAME, name);
		nullable(redirectUris).ifPresent(uris -> map.put(REDIRECT_URIS, uris));
		nullable(landingPage).ifPresent(lp -> map.put(LANDING_PAGE, lp));
		nullable(tokenValidity).ifPresent(tv -> map.put(TOKEN_VALIDITY, tv.toMinutes()));
		return map;
	}

	public String secret() {
		return secret;
	}


	public Set<String> redirectUris() {
		return redirectUris;
	}


	public Duration tokenValidity() {
		return tokenValidity;
	}

	public Client tokenValidity(Duration newValue) {
		this.tokenValidity = newValue;
		return this;
	}

	@Override
	public String toString() {
		return "Client["
		    + "id=" + id + ", "
		    + "landing_page=" + landingPage + ", "
		    + "name=" + name + ", "
		    + "secret=" + secret + ", "
		    + "redirectUris=" + redirectUris + ']';
	}
}
