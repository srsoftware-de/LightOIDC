/* © SRSoftware 2024 */
package de.srsoftware.oidc.api;

import java.util.List;
import java.util.Optional;

public interface UserService {
	public UserService	 delete(User user);
	public boolean passwordMatches(String password, String hashedPassword);
	public UserService	 init(User defaultUser);
	public List<User>	 list();
	public Optional<User>	 load(String id);
	public Optional<User>	 load(String username, String password);
	public <T extends UserService> T save(User user);
	public <T extends UserService> T updatePassword(User user, String plaintextPassword);
}
