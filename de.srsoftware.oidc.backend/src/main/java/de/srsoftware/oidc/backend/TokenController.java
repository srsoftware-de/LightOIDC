/* © SRSoftware 2024 */
package de.srsoftware.oidc.backend;

import static de.srsoftware.oidc.api.Constants.*;
import static java.lang.System.Logger.Level.*;
import static java.net.HttpURLConnection.HTTP_NOT_IMPLEMENTED;

import com.sun.net.httpserver.HttpExchange;
import de.srsoftware.oidc.api.*;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import org.jose4j.jwk.PublicJsonWebKey;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.lang.JoseException;

public class TokenController extends PathHandler {
	public record Configuration(String issuer, int tokenExpirationMinutes) {
	}
	private final ClientService	        clients;
	private final ClaimAuthorizationService authorizations;
	private final UserService	        users;
	private final KeyManager	        keyManager;
	private Configuration	        config;

	public TokenController(ClaimAuthorizationService authorizationService, ClientService clientService, KeyManager keyManager, UserService userService, Configuration configuration) {
		authorizations	= authorizationService;
		clients	= clientService;
		this.keyManager = keyManager;
		users	= userService;
		config	= configuration;
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
		return sendEmptyResponse(HTTP_NOT_IMPLEMENTED, ex);
		/*
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
		return sendContent(ex, response);*/
	}

	private String createJWT(Client client, User user) {
		try {
			PublicJsonWebKey key = keyManager.getKey();
			key.setUse("sig");
			JwtClaims claims = createIdTokenClaims(user, client);

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

	private JwtClaims createIdTokenClaims(User user, Client client) {
		JwtClaims claims = new JwtClaims();

		// required claims:
		claims.setIssuer(config.issuer);  // who creates the token and signs it
		claims.setSubject(user.uuid());	  // the subject/principal is whom the token is about
		claims.setAudience(client.id());
		claims.setExpirationTimeMinutesInTheFuture(config.tokenExpirationMinutes);  // time when the token will expire (10 minutes from now)
		claims.setIssuedAtToNow();			            // when the token was issued/created (now)

		claims.setClaim("client_id", client.id());
		claims.setClaim("email", user.email());  // additional claims/attributes about the subject can be added
		client.nonce().ifPresent(nonce -> claims.setClaim(NONCE, nonce));
		claims.setGeneratedJwtId();  // a unique identifier for the token
		return claims;
	}
}
