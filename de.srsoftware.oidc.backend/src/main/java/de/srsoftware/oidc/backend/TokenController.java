/* © SRSoftware 2024 */
package de.srsoftware.oidc.backend;

import static de.srsoftware.oidc.api.Constants.ATUH_CODE;
import static de.srsoftware.oidc.api.Constants.GRANT_TYPE;
import static java.lang.System.Logger.Level.ERROR;
import static java.lang.System.Logger.Level.WARNING;
import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;

import com.sun.net.httpserver.HttpExchange;
import de.srsoftware.oidc.api.PathHandler;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

public class TokenController extends PathHandler {
	private Map<String, String> deserialize(String body) {
		return Arrays.stream(body.split("&")).map(s -> s.split("=")).collect(Collectors.toMap(arr -> arr[0], arr -> arr[1]));
	}

	@Override
	public boolean doPost(String path, HttpExchange ex) throws IOException {
		// pre-login paths
		switch (path) {
			case "/":
				return provideToken(ex);
		}
		return notFound(ex);
	}

	private boolean provideToken(HttpExchange ex) throws IOException {
		var map = deserialize(body(ex));
		LOG.log(WARNING, "post data: {0}", map);
		LOG.log(ERROR, "{0}.provideToken(ex) not implemented!", getClass().getSimpleName());
		var grantType = map.get(GRANT_TYPE);
		if (!ATUH_CODE.equals(grantType)) sendContent(ex, HTTP_BAD_REQUEST, Map.of(ERROR, "unknown grant type", GRANT_TYPE, grantType));
		return sendEmptyResponse(HTTP_NOT_FOUND, ex);
	}
}
