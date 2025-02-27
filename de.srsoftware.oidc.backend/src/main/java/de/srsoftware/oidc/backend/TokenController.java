/* © SRSoftware 2024 */
package de.srsoftware.oidc.backend;

import static de.srsoftware.oidc.api.Constants.*;
import static de.srsoftware.oidc.api.Constants.ERROR;
import static de.srsoftware.utils.Optionals.emptyIfBlank;
import static java.net.HttpURLConnection.HTTP_UNAUTHORIZED;
import static java.nio.charset.StandardCharsets.US_ASCII;

import com.sun.net.httpserver.HttpExchange;
import de.srsoftware.http.PathHandler;
import de.srsoftware.oidc.api.*;
import de.srsoftware.oidc.api.data.AccessToken;
import de.srsoftware.oidc.api.data.Client;
import de.srsoftware.oidc.api.data.User;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.stream.Collectors;
import org.jose4j.jwk.PublicJsonWebKey;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.lang.JoseException;
import org.json.JSONObject;

public class TokenController extends PathHandler {
	public record Configuration(int tokenExpirationMinutes) {
	}
	private final ClientService	   clients;
	private final AuthorizationService authorizations;
	private final UserService	   users;
	private final KeyManager	   keyManager;
	private Configuration	   config;
	private static final Base64.Encoder BASE64 = Base64.getUrlEncoder();

	public TokenController(AuthorizationService authorizationService, ClientService clientService, KeyManager keyManager, UserService userService, Configuration configuration) {
		authorizations	= authorizationService;
		clients	= clientService;
		this.keyManager = keyManager;
		users	= userService;
		config	= configuration;
	}

	private String decode(String urlencoded) {
		return URLDecoder.decode(urlencoded, StandardCharsets.UTF_8);
	}

	private Map<String, String> deserialize(String body) {
		return Arrays
		    .stream(body.split("&"))  //
		    .map(s -> s.split("="))
		    .collect(Collectors.toMap(arr -> decode(arr[0]), arr -> decode(arr[1])));
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

	private HashMap<String, String> tokenResponse(String errorCode, String description) throws IOException {
		var map = new HashMap<String, String>();
		map.put(ERROR, errorCode);
		emptyIfBlank(description).ifPresent(d -> map.put(ERROR_DESCRIPTION, d));
		return map;
	}

	private boolean provideToken(HttpExchange ex) throws IOException {
		var map = deserialize(body(ex));

		var grantType = map.get(GRANT_TYPE);
		// verify grant type
		if (!AUTH_CODE.equals(grantType)) return badRequest(ex, tokenResponse(INVALID_GRANT, "unknown grant type \"%s\"".formatted(grantType)));

		var basicAuth = getBasicAuth(ex).orElse(null);

		var clientId  = basicAuth != null ? basicAuth.userId() : map.get(CLIENT_ID);
		var optClient = clients.getClient(clientId);
		if (optClient.isEmpty()) return badRequest(ex, tokenResponse(INVALID_CLIENT, "unknown client \"%s\"".formatted(clientId)));

		var client = optClient.get();
		if (client.secret() != null) {	// for confidential clients:
			// authenticate client by matching secret
			String clientSecret = basicAuth != null ? basicAuth.pass() : map.get(CLIENT_SECRET);
			if (clientSecret == null) return sendContent(ex, HTTP_UNAUTHORIZED, tokenResponse(INVALID_CLIENT, "client not authenticated"));
			if (!client.secret().equals(clientSecret)) return sendContent(ex, HTTP_UNAUTHORIZED, tokenResponse(INVALID_CLIENT, "client not authenticated"));
		}

		var authCode = map.get(CODE);

		// verify that code is not re-used
		var optAuthorization = authorizations.consumeAuthorization(authCode);
		if (optAuthorization.isEmpty()) return badRequest(ex, tokenResponse(INVALID_GRANT, "invalid auth code: \"%s\"".formatted(authCode)));
		var authorization = optAuthorization.get();

		// verify authorization code was issued to the authenticated client
		if (!authorization.clientId().equals(clientId)) return badRequest(ex, tokenResponse(UNAUTHORIZED_CLIENT, null));

		// verify redirect URI
		var uri = URLDecoder.decode(map.get(REDIRECT_URI), StandardCharsets.UTF_8);
		if (!client.redirectUris().contains(uri)) return badRequest(ex, tokenResponse(INVALID_REQUEST, "unknown redirect uri: \"%s\"".formatted(uri)));

		// verify user is valid
		var optUser = users.load(authorization.userId());
		if (optUser.isEmpty()) return badRequest(ex, tokenResponse(INVALID_REQUEST, "unknown user"));

		if (!authorization.scopes().scopes().contains(OPENID)) return badRequest(ex, tokenResponse(INVALID_REQUEST, "Token invalid for OpenID scope"));
		var user = optUser.get();

		var    accessToken = users.accessToken(user);
		var    issuer	   = hostname(ex);
		String jwToken	   = createJWT(client, user, accessToken, issuer);
		ex.getResponseHeaders().add("Cache-Control", "no-store");
		JSONObject response = new JSONObject();
		response.put(ACCESS_TOKEN, accessToken.id());
		response.put(TOKEN_TYPE, BEARER);
		response.put(EXPIRES_IN, 3600);
		response.put(ID_TOKEN, jwToken);

		return sendContent(ex, response);
	}

	String createJWT(Client client, User user, AccessToken accessToken, String issuer) {
		try {
			PublicJsonWebKey key    = keyManager.getKey();
			var	 algo   = key.getAlgorithm();
			var	 atHash = this.atHash(algo, accessToken);
			key.setUse("sig");
			JwtClaims claims = createIdTokenClaims(user, client, atHash, issuer);

			// A JWT is a JWS and/or a JWE with JSON claims as the payload.
			// In this example it is a JWS so we create a JsonWebSignature object.
			JsonWebSignature jws = new JsonWebSignature();

			jws.setHeader("typ", "JWT");
			jws.setPayload(claims.toJson());
			jws.setKey(key.getPrivateKey());
			jws.setKeyIdHeaderValue(key.getKeyId());
			jws.setAlgorithmHeaderValue(algo);

			return jws.getCompactSerialization();
		} catch (JoseException | KeyManager.KeyCreationException | IOException e) {
			throw new RuntimeException(e);
		}
	}

	private String atHash(String algo, AccessToken accessToken) {
		algo = "SHA" + algo.replaceAll("[^0-9]", "");
		try {
			var    digest = MessageDigest.getInstance(algo);
			byte[] hash   = digest.digest(accessToken.id().getBytes(US_ASCII));
			if (hash.length < 16) throw new RuntimeException("invalid hash (less than 128 bits)");
			if (hash.length > 16) {
				var trimmed = new byte[16];
				for (var i = 0; i < 16; i++) trimmed[i] = hash[i];
				hash = trimmed;
			}
			return BASE64.withoutPadding().encodeToString(hash);  // https://stackoverflow.com/a/30356461
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
	}

	private JwtClaims createIdTokenClaims(User user, Client client, String atHash, String issuer) {
		var       optNonce = authorizations.consumeNonce(user.uuid(), client.id());
		JwtClaims claims   = new JwtClaims();

		// required claims:
		claims.setIssuer(issuer);	 // who creates the token and signs it
		claims.setSubject(user.uuid());	 // the subject/principal is whom the token is about
		claims.setAudience(client.id());
		claims.setExpirationTimeMinutesInTheFuture(client.tokenValidity().toMinutes());	 // time when the token will expire (10 minutes from now)
		claims.setIssuedAtToNow();
		claims.setClaim(AT_HASH, atHash);
		claims.setClaim(CLIENT_ID, client.id());
		claims.setClaim(EMAIL, user.email());  // additional claims/attributes about the subject can be added
		claims.setClaim(USER, user.username());

		optNonce.ifPresent(nonce -> claims.setClaim(NONCE, nonce));
		claims.setGeneratedJwtId();  // a unique identifier for the token
		return claims;
	}
}
