/* © SRSoftware 2025 */
package de.srsoftware.oidc.backend;

import static de.srsoftware.oidc.api.Constants.*;
import static de.srsoftware.oidc.api.data.Permission.MANAGE_CLIENTS;
import static de.srsoftware.tools.Error.error;
import static java.net.HttpURLConnection.*;

import com.sun.net.httpserver.HttpExchange;
import de.srsoftware.oidc.api.*;
import de.srsoftware.oidc.api.data.AuthorizedScopes;
import de.srsoftware.oidc.api.data.Client;
import de.srsoftware.oidc.api.data.Session;
import de.srsoftware.oidc.api.data.User;
import de.srsoftware.tools.Optionals;
import de.srsoftware.tools.Path;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import org.json.JSONObject;

public class ClientController extends Controller {
	private static final System.Logger LOG = System.getLogger(ClientController.class.getSimpleName());
	private final AuthorizationService authorizations;
	private final ClientService	   clients;
	private final UserService	   users;
	private final TokenController	   tokens;

	public ClientController(AuthorizationService authorizationService, ClientService clientService, SessionService sessionService, UserService userService, TokenController tokenController) {
		super(sessionService);
		authorizations = authorizationService;
		clients        = clientService;
		users          = userService;
		tokens         = tokenController;
	}


	private boolean authorize(HttpExchange ex, Session session) throws IOException {
		var optUser = users.load(session.userId());
		if (optUser.isEmpty()) return invalidSessionUser(ex);
		var user  = optUser.get();
		var json  = json(ex);
		var state = json.has(STATE) ? json.getString(STATE) : null;
		if (!json.has(CLIENT_ID)) return badRequest(ex, error(ERROR_MISSING_PARAMETER).addData(PARAM, CLIENT_ID, STATE, state));
		var clientId  = json.getString(CLIENT_ID);
		var optClient = clients.getClient(clientId);
		if (optClient.isEmpty()) return badRequest(ex, error(ERROR_UNKNOWN_CLIENT).addData(CLIENT_ID, clientId, STATE, state));
		for (String param : List.of(SCOPE, RESPONSE_TYPE, REDIRECT_URI)) {
			if (!json.has(param)) return badRequest(ex, error(ERROR_MISSING_PARAMETER).addData(PARAM, param, STATE, state));
		}
		var scopes = toList(json, SCOPE);
		if (!scopes.contains(OPENID)) return badRequest(ex, error(ERROR_MISSING_PARAMETER).addData(PARAM, "Scope: openid", STATE, state));
		var responseTypes = toList(json, RESPONSE_TYPE);
		var types	  = 0;
		for (var responseType : responseTypes) {
			switch (responseType) {
				case CODE:
				case ID_TOKEN:
					types++;
					break;
				default:
					return badRequest(ex, error(ERROR_UNSUPPORTED_RESPONSE_TYPE).addData(RESPONSE_TYPE, responseType, STATE, state));
			}
		}
		if (types < 1) return badRequest(ex, error(ERROR_MISSING_CODE_RESPONSE_TYPE).addData(STATE, state));

		var client   = optClient.get();
		var redirect = json.getString(REDIRECT_URI);

		if (!client.redirectUris().contains(redirect)) return badRequest(ex, error(ERROR_INVALID_REDIRECT).addData(REDIRECT_URI, redirect, STATE, state));

		if (json.has(AUTHORZED)) {  // user did consent
			var authorized = json.getJSONObject(AUTHORZED);
			var days       = authorized.getInt("days");
			var list       = new ArrayList<String>();
			authorized.getJSONArray("scopes").forEach(scope -> list.add(scope.toString()));
			authorizations.authorize(user.uuid(), client.id(), list, Instant.now().plus(days, ChronoUnit.DAYS));
		}
		if (json.has(NONCE)) authorizations.nonce(user.uuid(), client.id(), json.getString(NONCE));

		var authResult = authorizations.getAuthorization(user.uuid(), client.id(), scopes);
		if (!authResult.unauthorizedScopes().isEmpty()) {
			return sendContent(ex, Map.of("unauthorized_scopes", authResult.unauthorizedScopes(), "rp", client.name()));
		}

		var joinedAuthorizedScopes = Optionals.nullable(authResult.authorizedScopes()).map(AuthorizedScopes::scopes).map(list -> String.join(" ", list));

		var result = new HashMap<String, Object>();

		joinedAuthorizedScopes.ifPresent(authorizedScopes -> result.put(SCOPE, authorizedScopes));

		if (responseTypes.contains(ID_TOKEN)) {
			var    accessToken = users.accessToken(user);
			var    issuer	   = hostname(ex);
			String jwToken	   = tokens.createJWT(client, user, accessToken, issuer);
			ex.getResponseHeaders().add("Cache-Control", "no-store");
			result.put(ACCESS_TOKEN, accessToken.id());
			result.put(TOKEN_TYPE, BEARER);
			result.put(EXPIRES_IN, 3600);
			result.put(ID_TOKEN, jwToken);
		} else if (responseTypes.contains(CODE)) {
			result.put(CODE, authResult.authCode());
			if (state != null) result.put(STATE, state);
		}


		return sendContent(ex, result);
	}

	private List<String> toList(JSONObject json, String key) {
		return Arrays.asList(json.getString(key).split(" "));
	}

	private boolean deleteClient(HttpExchange ex, Session session) throws IOException {
		var optUser = users.load(session.userId());
		if (optUser.isEmpty()) return invalidSessionUser(ex);
		if (!optUser.get().hasPermission(MANAGE_CLIENTS)) return badRequest(ex, "NOT ALLOWED");
		var json = json(ex);
		var id   = json.getString(CLIENT_ID);
		clients.remove(id);
		return sendEmptyResponse(HTTP_OK, ex);
	}


	@Override
	public boolean doDelete(Path path, HttpExchange ex) throws IOException {
		var optSession = getSession(ex);
		if (optSession.isEmpty()) return sendEmptyResponse(HTTP_UNAUTHORIZED, ex);

		// post-login paths
		var session = optSession.get();
		var optUser = users.load(session.userId());
		if (optUser.isEmpty()) return invalidSessionUser(ex);
		var user = optUser.get();
		sessions.extend(session, user);

		if (path.isEmpty())	return deleteClient(ex, session);
		return notFound(ex);
	}

	@Override
	public boolean doGet(Path path, HttpExchange ex) throws IOException {
		var optSession = getSession(ex);
		if (optSession.isEmpty()) return sendContent(ex, HTTP_UNAUTHORIZED, "No authorized!");

		// post-login paths
		var session = optSession.get();
		var optUser = users.load(session.userId());
		if (optUser.isEmpty()) return invalidSessionUser(ex);
		var user = optUser.get();
		sessions.extend(session, user);

		switch (path.pop()) {
			case "dash":
				return dashboard(ex, user);
			case "list":
				return list(ex, session);
		}
		return notFound(ex);
	}

	private boolean dashboard(HttpExchange ex, User user) throws IOException {
		var authorizedClients = authorizations  //
			            .authorizedClients(user.uuid())
			            .stream()
			            .map(clients::getClient)
			            .flatMap(Optional::stream)
			            .sorted(Comparator.comparing(Client::name, String.CASE_INSENSITIVE_ORDER))
			            .map(Client::safeMap)
			            .toList();
		return sendContent(ex, Map.of(AUTHORZED, authorizedClients, NAME, user.realName()));
	}


	@Override
	public boolean doPost(Path path, HttpExchange ex) throws IOException {
		var optSession = getSession(ex);
		if (optSession.isEmpty()) return sendContent(ex, HTTP_UNAUTHORIZED, "No authorized!");

		// post-login paths
		var session = optSession.get();
		var optUser = users.load(session.userId());
		if (optUser.isEmpty()) return invalidSessionUser(ex);
		var user = optUser.get();
		sessions.extend(session, user);

		if (path.isEmpty()) return load(ex,session);
		switch (path.pop()) {
			case "add", "update":
				return save(ex, session);
			case "authorize":
				return authorize(ex, session);
		}
		return notFound(ex);
	}

	private boolean list(HttpExchange ex, Session session) throws IOException {
		var optUser = users.load(session.userId());
		if (optUser.isEmpty()) return invalidSessionUser(ex);
		if (!optUser.get().hasPermission(MANAGE_CLIENTS)) return sendEmptyResponse(HTTP_FORBIDDEN, ex);
		var json = new JSONObject();
		clients.listClients().forEach(client -> json.put(client.id(), client.map()));
		return sendContent(ex, json);
	}


	private boolean load(HttpExchange ex, Session session) throws IOException {
		var optUser = users.load(session.userId());
		if (optUser.isEmpty()) return invalidSessionUser(ex);
		if (!optUser.get().hasPermission(MANAGE_CLIENTS)) return sendEmptyResponse(HTTP_FORBIDDEN, ex);
		var json = json(ex);
		if (json.has(CLIENT_ID)) {
			var clientID = json.getString(CLIENT_ID);
			var client   = clients.getClient(clientID).map(Client::map).map(JSONObject::new);
			if (client.isPresent()) return sendContent(ex, client.get());
		}
		return sendEmptyResponse(HTTP_NOT_FOUND, ex);
	}

	private boolean save(HttpExchange ex, Session session) throws IOException {
		var optUser = users.load(session.userId());
		if (optUser.isEmpty()) return invalidSessionUser(ex);
		if (!optUser.get().hasPermission(MANAGE_CLIENTS)) return badRequest(ex, "NOT ALLOWED");
		var json      = json(ex);
		var redirects = new HashSet<String>();
		for (Object o : json.getJSONArray(REDIRECT_URIS)) {
			if (o instanceof String s) redirects.add(s);
		}
		var landingPage	   = json.has(LANDING_PAGE) ? json.getString(LANDING_PAGE) : null;
		var token_duration = Duration.ofMinutes(json.has(TOKEN_VALIDITY) ? json.getLong(TOKEN_VALIDITY) : 10);
		var client	   = new Client(json.getString(CLIENT_ID), json.getString(NAME), json.getString(SECRET), redirects).landingPage(landingPage).tokenValidity(token_duration);
		clients.save(client);
		return sendContent(ex, client);
	}
}
