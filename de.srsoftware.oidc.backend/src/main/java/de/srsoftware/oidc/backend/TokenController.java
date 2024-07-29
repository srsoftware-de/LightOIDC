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
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;
import org.jose4j.jws.AlgorithmIdentifiers;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.keys.HmacKey;
import org.jose4j.lang.JoseException;
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
		try {
			byte[]  secretBytes = client.secret().getBytes(StandardCharsets.UTF_8);
			HmacKey hmacKey	    = new HmacKey(secretBytes);

			JwtClaims claims = new JwtClaims();
			claims.setIssuer("Issuer");		 // who creates the token and signs it
			claims.setAudience("Audience");		 // to whom the token is intended to be sent
			claims.setExpirationTimeMinutesInTheFuture(10);	 // time when the token will expire (10 minutes from now)
			claims.setGeneratedJwtId();		 // a unique identifier for the token
			claims.setIssuedAtToNow();		 // when the token was issued/created (now)
			claims.setNotBeforeMinutesInThePast(2);	 // time before which the token is not yet valid (2 minutes ago)
			claims.setSubject("subject");		 // the subject/principal is whom the token is about
			claims.setClaim("email", "mail@example.com");	 // additional claims/attributes about the subject can be added
			List<String> groups = Arrays.asList("group-one", "other-group", "group-three");
			claims.setStringListClaim("groups", groups);  // multi-valued claims work too and will end up as a JSON array

			// A JWT is a JWS and/or a JWE with JSON claims as the payload.
			// In this example it is a JWS so we create a JsonWebSignature object.
			JsonWebSignature jws = new JsonWebSignature();
			if (secretBytes.length * 8 < 256) {
				LOG.log(WARNING, "Using secret with less than 256 bits! You will go to hell for this!");
				jws.setDoKeyValidation(false);	// TODO: this is dangerous! Better: enforce key length of 256bits!
			}

			jws.setPayload(claims.toJson());
			jws.setKey(hmacKey);
			jws.setAlgorithmHeaderValue(AlgorithmIdentifiers.HMAC_SHA256);
			return jws.getCompactSerialization();
		} catch (JoseException e) {
			throw new RuntimeException(e);
		}
	}
}
