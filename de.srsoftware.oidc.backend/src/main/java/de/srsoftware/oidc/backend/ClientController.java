/* © SRSoftware 2024 */
package de.srsoftware.oidc.backend;

import static de.srsoftware.oidc.api.Constants.*;
import static de.srsoftware.oidc.api.Permission.MANAGE_CLIENTS;
import static java.lang.System.Logger.Level.ERROR;
import static java.net.HttpURLConnection.*;

import com.sun.net.httpserver.HttpExchange;
import de.srsoftware.oidc.api.*;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;
import org.json.JSONObject;

public class ClientController extends Controller {
	private static final System.Logger LOG = System.getLogger(ClientController.class.getSimpleName());
	private final AuthorizationService authorizations;
	private final ClientService	   clients;

	public ClientController(AuthorizationService authorizationService, ClientService clientService, SessionService sessionService) {
		super(sessionService);
		authorizations = authorizationService;
		clients        = clientService;
	}


	private boolean authorize(HttpExchange ex, Session session) throws IOException {
		var user  = session.user();
		var json  = json(ex);
		var scope = json.getString(SCOPE);
		if (!Arrays.asList(scope.split(" ")).contains(OPENID)) return sendContent(ex, HTTP_BAD_REQUEST, Map.of(ERROR, "openid scope missing in request"));

		var clientId  = json.getString(CLIENT_ID);
		var redirect  = json.getString(REDIRECT_URI);
		var optClient = clients.getClient(clientId);
		if (optClient.isEmpty()) return badRequest(ex, Map.of(CAUSE, "unknown client", CLIENT_ID, clientId));
		var client = optClient.get();

		if (!client.redirectUris().contains(redirect)) return badRequest(ex, Map.of(CAUSE, "unknown redirect uri", REDIRECT_URI, redirect));

		if (!authorizations.isAuthorized(client, session.user())) {
			if (json.has(DAYS)) {
				var days       = json.getInt(DAYS);
				var expiration = Instant.now().plus(Duration.ofDays(days));
				authorizations.authorize(client, user, expiration);
			} else {
				return sendContent(ex, Map.of(CONFIRMED, false, NAME, client.name()));
			}
		}
		var state = json.getString(STATE);
		var code  = UUID.randomUUID().toString();
		authorizations.addCode(client, session.user(), code);
		return sendContent(ex, Map.of(CONFIRMED, true, CODE, code, REDIRECT_URI, redirect, STATE, state));
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
		if (optSession.isEmpty()) return sendEmptyResponse(HTTP_UNAUTHORIZED, ex);

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
