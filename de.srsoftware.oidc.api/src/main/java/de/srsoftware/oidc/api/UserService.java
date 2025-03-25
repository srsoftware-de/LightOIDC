/* © SRSoftware 2025 */
package de.srsoftware.oidc.api;

import static java.util.Optional.empty;

import de.srsoftware.oidc.api.data.AccessToken;
import de.srsoftware.oidc.api.data.Lock;
import de.srsoftware.oidc.api.data.User;
import de.srsoftware.tools.result.Result;
import java.time.Instant;
import java.util.*;

public interface UserService {
	Map<String, Lock> failedLogins = new HashMap<>();

	/**
	 * create a new access token for a given user
	 * @param user
	 * @return
	 */
	public AccessToken    accessToken(User user);
	public Optional<User> consumeToken(String accessToken);
	public UserService    delete(User user);

	/**
	 * return the user identified by its access token
	 * @param accessToken
	 * @return
	 */
	public Optional<User>         forToken(String accessToken);
	public UserService            init(User defaultUser);
	public List<User>             list();
	public Set<User>              find(String idOrEmail);
	public default Optional<Lock> getLock(String key) {
		var failedLogin = failedLogins.get(key);
		if (failedLogin == null || failedLogin.releaseTime().isBefore(Instant.now())) return empty();
		return Optional.of(failedLogin);
	}
	public Optional<User> load(String id);
	public Result<User>   login(String username, String password);
	public default Lock   lock(String key) {
		  return failedLogins.computeIfAbsent(key, k -> new Lock()).count();
	}
	public boolean	           passwordMatches(String plaintextPassword, User user);
	public UserService         save(User user);
	public default UserService unlock(String key) {
		failedLogins.remove(key);
		return this;
	}
	public UserService updatePassword(User user, String plaintextPassword);
}
