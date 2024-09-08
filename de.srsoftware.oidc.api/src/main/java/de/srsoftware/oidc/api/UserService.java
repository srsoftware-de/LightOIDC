/* © SRSoftware 2024 */
package de.srsoftware.oidc.api;

import de.srsoftware.oidc.api.data.AccessToken;
import de.srsoftware.oidc.api.data.User;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface UserService {
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
	public Optional<User>	 forToken(String accessToken);
	public UserService	 init(User defaultUser);
	public List<User>	 list();
	public Set<User>	 find(String idOrEmail);
	public Optional<User>	 load(String id);
	public Optional<User>	 load(String username, String password);
	public boolean		 passwordMatches(String password, String hashedPassword);
	public <T extends UserService> T save(User user);
	public <T extends UserService> T updatePassword(User user, String plaintextPassword);
}
