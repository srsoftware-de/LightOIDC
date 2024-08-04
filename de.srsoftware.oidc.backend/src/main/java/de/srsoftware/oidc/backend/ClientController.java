/* © SRSoftware 2024 */
package de.srsoftware.oidc.backend;

import static de.srsoftware.oidc.api.Constants.*;
import static de.srsoftware.oidc.api.Permission.MANAGE_CLIENTS;
import static java.net.HttpURLConnection.*;

import com.sun.net.httpserver.HttpExchange;
import de.srsoftware.oidc.api.*;
import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import org.json.JSONObject;

public class ClientController extends Controller {
	private static final System.Logger      LOG = System.getLogger(ClientController.class.getSimpleName());
	private final ClaimAuthorizationService authorizations;
	private final ClientService	        clients;

	public ClientController(ClaimAuthorizationService authorizationService, ClientService clientService, SessionService sessionService) {
		super(sessionService);
		authorizations = authorizationService;
		clients        = clientService;
	}


	private boolean authorize(HttpExchange ex, Session session) throws IOException {
		var user = session.user();
		var json = json(ex);
		for (String param : List.of(SCOPE, RESPONSE_TYPE, CLIENT_ID, REDIRECT_URI)) {
			if (!json.has(param)) return badRequest(ex, "Missing required parameter \"%s\"!".formatted(param));
		}
		var scopes = toList(json, SCOPE);
		if (!scopes.contains(OPENID)) return badRequest(ex, "This is an OpenID Provider. You should request \"openid\" scope!");
		var responseTypes = toList(json, RESPONSE_TYPE);
		for (var responseType : responseTypes) {
			switch (responseType) {
				case ID_TOKEN:
				case TOKEN:
					return sendContent(ex, HTTP_NOT_IMPLEMENTED, "Response type \"%s\" currently not supported".formatted(responseType));
				case CODE:
					break;
				default:
					return badRequest(ex, "Unknown response type \"%s\"".formatted(responseType));
			}
		}
		if (!responseTypes.contains(CODE)) return badRequest(ex, "Sorry, at the moment I can only handle \"%s\" response type".formatted(CODE));

		var clientId  = json.getString(CLIENT_ID);
		var optClient = clients.getClient(clientId);
		if (optClient.isEmpty()) return badRequest(ex, Map.of(CAUSE, "unknown client", CLIENT_ID, clientId));
		var client   = optClient.get();
		var redirect = json.getString(REDIRECT_URI);
		if (!client.redirectUris().contains(redirect)) return badRequest(ex, Map.of(CAUSE, "unknown redirect uri", REDIRECT_URI, redirect));
		var state = json.has(STATE) ? json.getString(STATE) : null;

		client.nonce(json.has(NONCE) ? json.getString(NONCE) : null);
		if (json.has(AUTHORZED)) {
			var authorized = json.getJSONObject(AUTHORZED);
			var days       = authorized.getInt("days");
			var list       = new ArrayList<String>();
			authorized.getJSONArray("scopes").forEach(scope -> list.add(scope.toString()));
			authorizations.authorize(user, client, list, Instant.now().plus(days, ChronoUnit.DAYS));
		}

		var authResult = authorizations.getAuthorization(user, client, scopes);
		if (!authResult.unauthorizedScopes().isEmpty()) {
			return sendContent(ex, Map.of("unauthorized_scopes", authResult.unauthorizedScopes(), "rp", client.name()));
		}
		var authoriedScopes = authResult.authorizedScopes().stream().map(AuthorizedScope::scope).collect(Collectors.joining(" "));
		var result	    = new HashMap<String, String>();
		result.put(SCOPE, authoriedScopes);
		result.put(CODE, authResult.authCode());
		if (state != null) result.put(STATE, state);
		return sendContent(ex, result);
	}

	private List<String> toList(JSONObject json, String key) {
		return Arrays.asList(json.getString(key).split(" "));
	}

	private boolean deleteClient(HttpExchange ex, Session session) throws IOException {
		if (!session.user().hasPermission(MANAGE_CLIENTS)) return badRequest(ex, "NOT ALLOWED");
		var json = json(ex);
		var id   = json.getString(CLIENT_ID);
		clients.getClient(id).ifPresent(clients::remove);
		return sendEmptyResponse(HTTP_OK, ex);
	}


	@Override
	public boolean doDelete(String path, HttpExchange ex) throws IOException {
		var optSession = getSession(ex);
		if (optSession.isEmpty()) return sendEmptyResponse(HTTP_UNAUTHORIZED, ex);

		// post-login paths
		var session = optSession.get();
		switch (path) {
			case "/":
				return deleteClient(ex, session);
		}
		return notFound(ex);
	}


	@Override
	public boolean doPost(String path, HttpExchange ex) throws IOException {
		var optSession = getSession(ex);
		if (optSession.isEmpty()) return sendContent(ex, HTTP_UNAUTHORIZED, "No authorized!");

		// post-login paths
		var session = optSession.get();
		switch (path) {
			case "/":
				return load(ex, session);
			case "/add", "/update":
				return save(ex, session);
			case "/authorize":
				return authorize(ex, session);
			case "/list":
				return list(ex, session);
		}
		return notFound(ex);
	}

	private boolean list(HttpExchange ex, Session session) throws IOException {
		var user = session.user();
		if (!user.hasPermission(MANAGE_CLIENTS)) return sendEmptyResponse(HTTP_FORBIDDEN, ex);
		var json = new JSONObject();
		clients.listClients().forEach(client -> json.put(client.id(), Map.of("name", client.name(), "redirect_uris", client.redirectUris())));
		return sendContent(ex, json);
	}


	private boolean load(HttpExchange ex, Session session) throws IOException {
		if (!session.user().hasPermission(MANAGE_CLIENTS)) return sendEmptyResponse(HTTP_FORBIDDEN, ex);
		var json = json(ex);
		if (json.has(CLIENT_ID)) {
			var clientID = json.getString(CLIENT_ID);
			var client   = clients.getClient(clientID).map(Client::map).map(JSONObject::new);
			if (client.isPresent()) return sendContent(ex, client.get());
		}
		return sendEmptyResponse(HTTP_NOT_FOUND, ex);
	}

	private boolean save(HttpExchange ex, Session session) throws IOException {
		if (!session.user().hasPermission(MANAGE_CLIENTS)) return badRequest(ex, "NOT ALLOWED");
		var json      = json(ex);
		var redirects = new HashSet<String>();
		for (Object o : json.getJSONArray(REDIRECT_URIS)) {
			if (o instanceof String s) redirects.add(s);
		}
		var client = new Client(json.getString(CLIENT_ID), json.getString(NAME), json.getString(SECRET), redirects);
		clients.save(client);
		return sendContent(ex, client);
	}
}
