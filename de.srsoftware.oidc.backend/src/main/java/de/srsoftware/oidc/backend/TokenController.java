/* © SRSoftware 2024 */
package de.srsoftware.oidc.backend;

import static de.srsoftware.oidc.api.Constants.*;
import static de.srsoftware.utils.Optionals.nullable;
import static java.lang.System.Logger.Level.*;
import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;

import com.sun.net.httpserver.HttpExchange;
import de.srsoftware.oidc.api.*;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;
import org.jose4j.jwk.PublicJsonWebKey;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.lang.JoseException;
import org.json.JSONObject;

public class TokenController extends PathHandler {
	private final ClientService	   clients;
	private final AuthorizationService authorizations;
	private final UserService	   users;
	private final KeyManager	   keyManager;

	public TokenController(AuthorizationService authorizationService, ClientService clientService, KeyManager keyManager, UserService userService) {
		authorizations	= authorizationService;
		clients	= clientService;
		this.keyManager = keyManager;
		users	= userService;
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
		// TODO: check 	data, → https://openid.net/specs/openid-connect-core-1_0.html#TokenEndpoint

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

		if (client.secret() != null) {
			String clientSecret = nullable(ex.getRequestHeaders().get(AUTHORIZATION)).map(list -> list.get(0)).filter(s -> s.startsWith("Basic ")).map(s -> s.substring(6)).map(s -> Base64.getDecoder().decode(s)).map(bytes -> new String(bytes, StandardCharsets.UTF_8)).filter(s -> s.startsWith("%s:".formatted(client.id()))).map(s -> s.substring(client.id().length() + 1).trim()).orElseGet(() -> map.get(CLIENT_SECRET));
			if (clientSecret == null) return sendContent(ex, HTTP_BAD_REQUEST, Map.of(ERROR, "client secret missing"));
			if (!client.secret().equals(clientSecret)) return sendContent(ex, HTTP_BAD_REQUEST, Map.of(ERROR, "client secret mismatch"));
		}
		String jwToken = createJWT(client, user.get());
		ex.getResponseHeaders().add("Cache-Control", "no-store");
		JSONObject response = new JSONObject();
		response.put(ACCESS_TOKEN, users.accessToken(user.get()));
		response.put(TOKEN_TYPE, BEARER);
		response.put(EXPIRES_IN, 3600);
		response.put(ID_TOKEN, jwToken);
		LOG.log(DEBUG, jwToken);
		return sendContent(ex, response);
	}

	private String createJWT(Client client, User user) {
		try {
			PublicJsonWebKey key = keyManager.getKey();
			key.setUse("sig");
			JwtClaims claims = getJwtClaims(user, client);

			// A JWT is a JWS and/or a JWE with JSON claims as the payload.
			// In this example it is a JWS so we create a JsonWebSignature object.
			JsonWebSignature jws = new JsonWebSignature();

			jws.setHeader("typ", "JWT");
			jws.setPayload(claims.toJson());
			jws.setKey(key.getPrivateKey());
			jws.setKeyIdHeaderValue(key.getKeyId());
			jws.setAlgorithmHeaderValue(key.getAlgorithm());

			return jws.getCompactSerialization();
		} catch (JoseException | KeyManager.KeyCreationException | IOException e) {
			throw new RuntimeException(e);
		}
	}

	private static JwtClaims getJwtClaims(User user, Client client) {
		JwtClaims claims = new JwtClaims();
		claims.setAudience(client.id(), "test");
		claims.setClaim("client_id", client.id());
		claims.setClaim("email", user.email());	      // additional claims/attributes about the subject can be added
		claims.setExpirationTimeMinutesInTheFuture(10);	      // time when the token will expire (10 minutes from now)
		claims.setIssuedAtToNow();		      // when the token was issued/created (now)
		claims.setIssuer("https://lightoidc.srsoftware.de");  // who creates the token and signs it
		claims.setGeneratedJwtId();		      // a unique identifier for the token
		claims.setSubject(user.uuid());		      // the subject/principal is whom the token is about
		client.nonce().ifPresent(nonce -> claims.setClaim(NONCE, nonce));
		return claims;
	}
}
