/* © SRSoftware 2024 */
package de.srsoftware.utils;


import java.util.HashMap;
import java.util.Map;
import org.json.JSONObject;

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

	public static <T> Error<T> message(String cause, Object... tokens) {
		var err      = new Error<T>(cause);
		err.metadata = new HashMap<>();
		for (int i = 0; i < tokens.length - 1; i += 2) {
			err.metadata.put(tokens[i].toString(), tokens[i + 1]);
		}
		return err;
	}

	public JSONObject json() {
		var json = new JSONObject(Map.of("error", cause));
		if (metadata != null) json.put("metadata", metadata);
		return json;
	}
}
