/* © SRSoftware 2024 */
package de.srsoftware.oidc.api;

import java.util.List;
import java.util.Optional;

public interface UserService {
	/**
	 * create a new access token for a given user
	 * @param user
	 * @return
	 */
	public String	   accessToken(User user);
	public UserService delete(User user);

	/**
	 * return the user identified by its access token
	 * @param accessToken
	 * @return
	 */
	public Optional<User>	 forToken(String accessToken);
	public UserService	 init(User defaultUser);
	public List<User>	 list();
	public Optional<User>	 load(String id);
	public Optional<User>	 load(String username, String password);
	public boolean		 passwordMatches(String password, String hashedPassword);
	public <T extends UserService> T save(User user);
	public <T extends UserService> T updatePassword(User user, String plaintextPassword);
}
