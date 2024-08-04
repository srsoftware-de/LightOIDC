/* © SRSoftware 2024 */
package de.srsoftware.http;

import static de.srsoftware.utils.Optionals.nullable;
import static java.lang.System.Logger.Level.*;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public abstract class Cookie implements Map.Entry<String, String> {
	static final System.Logger LOG = System.getLogger(SessionToken.class.getSimpleName());
	private final String       key;
	private String	           value = null;

	Cookie(String key, String value) {
		this.key = key;
		setValue(value);
	}

	public <T extends Cookie> T addTo(Headers headers) {
		LOG.log(INFO, "sending cookie {0}={1}", key, value);
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

	protected static List<String> of(HttpExchange ex) {
		return nullable(ex.getRequestHeaders().get("Cookie")).stream().flatMap(List::stream).flatMap(s -> Arrays.stream(s.split(";"))).map(String::trim).peek(cookie -> LOG.log(INFO, "received cookie {0}", cookie)).toList();
	}

	@Override
	public String setValue(String s) {
		var oldVal = value;
		value      = s;
		return oldVal;
	}
}
