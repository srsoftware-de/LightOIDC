/* © SRSoftware 2024 */
package de.srsoftware.oidc.backend;

import static de.srsoftware.oidc.api.Constants.*;
import static java.lang.System.Logger.Level.*;
import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;

import com.sun.net.httpserver.HttpExchange;
import de.srsoftware.oidc.api.Client;
import de.srsoftware.oidc.api.ClientService;
import de.srsoftware.oidc.api.PathHandler;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import org.json.JSONObject;

public class TokenController extends PathHandler {
	private final ClientService clients;

	public TokenController(ClientService clientService) {
		clients = clientService;
	}

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
		// TODO: check 	Authorization Code, → https://openid.net/specs/openid-connect-core-1_0.html#TokenEndpoint
		// TODO: check Redirect URL
		LOG.log(DEBUG, "post data: {0}", map);
		LOG.log(WARNING, "{0}.provideToken(ex) not implemented!", getClass().getSimpleName());
		var grantType = map.get(GRANT_TYPE);
		if (!ATUH_CODE.equals(grantType)) sendContent(ex, HTTP_BAD_REQUEST, Map.of(ERROR, "unknown grant type", GRANT_TYPE, grantType));
		var optClient = Optional.ofNullable(map.get(CLIENT_ID)).flatMap(clients::getClient);
		if (optClient.isEmpty()) {
			LOG.log(ERROR, "client not found");
			return sendEmptyResponse(HTTP_BAD_REQUEST, ex);
			// TODO: send correct response
		}
		var secretFromClient = map.get(CLIENT_SECRET);
		var client	     = optClient.get();
		if (!client.secret().equals(secretFromClient)) {
			LOG.log(ERROR, "client secret mismatch");
			return sendEmptyResponse(HTTP_BAD_REQUEST, ex);
			// TODO: send correct response
		}
		String jwToken = createJWT(client);
		ex.getResponseHeaders().add("Cache-Control", "no-store");
		JSONObject response = new JSONObject();
		response.put(ACCESS_TOKEN, UUID.randomUUID().toString());  // TODO: wofür genau wird der verwendet, was gilt es hier zu beachten
		response.put(TOKEN_TYPE, BEARER);
		response.put(EXPIRES_IN, 3600);
		response.put(ID_TOKEN, jwToken);
		return sendContent(ex, response);
	}

	private String createJWT(Client client) {
		return null;
	}
}
