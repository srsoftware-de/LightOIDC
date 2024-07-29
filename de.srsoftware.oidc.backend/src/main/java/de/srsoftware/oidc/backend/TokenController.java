/* © SRSoftware 2024 */
package de.srsoftware.oidc.backend;

import static de.srsoftware.oidc.api.Constants.*;
import static java.lang.System.Logger.Level.*;
import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;

import com.sun.net.httpserver.HttpExchange;
import de.srsoftware.oidc.api.*;
import java.io.IOException;
import java.net.URLDecoder;
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
	private final ClientService	   clients;
	private final AuthorizationService authorizations;
	private final UserService	   users;

	public TokenController(AuthorizationService authorizationService, ClientService clientService, UserService userService) {
		authorizations = authorizationService;
		clients        = clientService;
		users          = userService;
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
		var map       = deserialize(body(ex));
		var grantType = map.get(GRANT_TYPE);
		if (!AUTH_CODE.equals(grantType)) return sendContent(ex, HTTP_BAD_REQUEST, Map.of(ERROR, "unknown grant type", GRANT_TYPE, grantType));

		var code	     = map.get(CODE);
		var optAuthorization = authorizations.forCode(code);
		if (optAuthorization.isEmpty()) return sendContent(ex, HTTP_BAD_REQUEST, Map.of(ERROR, "invalid auth code", CODE, code));
		var authorization = optAuthorization.get();

		var clientId = map.get(CLIENT_ID);
		if (!authorization.clientId().equals(clientId)) return sendContent(ex, HTTP_BAD_REQUEST, Map.of(ERROR, "invalid client id", CLIENT_ID, clientId));
		var optClient = clients.getClient(clientId);
		if (optClient.isEmpty()) return sendContent(ex, HTTP_BAD_REQUEST, Map.of(ERROR, "unknown client", CLIENT_ID, clientId));
		var client = optClient.get();

		var user = users.load(authorization.userId());
		if (user.isEmpty()) return sendContent(ex, 500, Map.of(ERROR, "User not found"));

		var uri = URLDecoder.decode(map.get(REDIRECT_URI), StandardCharsets.UTF_8);
		if (!client.redirectUris().contains(uri)) sendContent(ex, HTTP_BAD_REQUEST, Map.of(ERROR, "unknown redirect uri", REDIRECT_URI, uri));

		var secretFromClient = map.get(CLIENT_SECRET);
		if (!client.secret().equals(secretFromClient)) return sendContent(ex, HTTP_BAD_REQUEST, Map.of(ERROR, "client secret mismatch"));

		String jwToken = createJWT(client, user.get());
		ex.getResponseHeaders().add("Cache-Control", "no-store");
		JSONObject response = new JSONObject();
		response.put(ACCESS_TOKEN, UUID.randomUUID().toString());  // TODO: wofür genau wird der verwendet, was gilt es hier zu beachten
		response.put(TOKEN_TYPE, BEARER);
		response.put(EXPIRES_IN, 3600);
		response.put(ID_TOKEN, jwToken);
		return sendContent(ex, response);
	}

	private String createJWT(Client client, User user) {
		try {
			byte[]  secretBytes = client.secret().getBytes(StandardCharsets.UTF_8);
			HmacKey hmacKey	    = new HmacKey(secretBytes);

			JwtClaims claims = getJwtClaims(user);

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

	private static JwtClaims getJwtClaims(User user) {
		JwtClaims claims = new JwtClaims();
		claims.setIssuer(APP_NAME);		 // who creates the token and signs it
		claims.setExpirationTimeMinutesInTheFuture(10);	 // time when the token will expire (10 minutes from now)
		claims.setGeneratedJwtId();		 // a unique identifier for the token
		claims.setIssuedAtToNow();		 // when the token was issued/created (now)
		claims.setNotBeforeMinutesInThePast(2);	 // time before which the token is not yet valid (2 minutes ago)
		claims.setSubject(user.uuid());		 // the subject/principal is whom the token is about
		claims.setClaim("email", user.email());	 // additional claims/attributes about the subject can be added
		return claims;
	}
}
