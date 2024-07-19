/* © SRSoftware 2024 */
package de.srsoftware.cookies;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public abstract class Cookie implements Map.Entry<String, String> {
	private final String key;
	private String	     value = null;

	Cookie(String key, String value) {
		this.key = key;
		setValue(value);
	}

	public <T extends Cookie> T addTo(Headers headers) {
		headers.add("Set-Cookie", "%s=%s".formatted(key, value));
		return (T)this;
	}

	public <T extends Cookie> T addTo(HttpExchange ex) {
		return this.addTo(ex.getResponseHeaders());
	}

	@Override
	public String getKey() {
		return key;
	}

	@Override
	public String getValue() {
		return value;
	}

	protected static Optional<List<String>> of(HttpExchange ex) {
		return Optional.ofNullable(ex.getRequestHeaders().get("Cookie"));
	}

	@Override
	public String setValue(String s) {
		var oldVal = value;
		value      = s;
		return oldVal;
	}
}
