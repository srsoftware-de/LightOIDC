/* © SRSoftware 2024 */
package de.srsoftware.oidc.api;


import java.util.HashMap;
import java.util.Map;

public class Error<T> implements Result<T> {
	private final String        cause;
	private Map<String, Object> metadata;

	public Error(String cause) {
		this.cause = cause;
	}

	public String cause() {
		return cause;
	}

	@Override
	public boolean isError() {
		return true;
	}

	public static <T> Error<T> message(String text) {
		return new Error<T>(text);
	}

	public Error<T> metadata(Object... tokens) {
		metadata = new HashMap<String, Object>();
		for (int i = 0; i < tokens.length - 1; i += 2) {
			metadata.put(tokens[i].toString(), tokens[i + 1]);
		}
		return this;
	}
}
