/* © SRSoftware 2024 */
package de.srsoftware.oidc.api;

import java.util.List;
import java.util.Optional;

public interface UserService {
	public UserService    delete(User user);
	public UserService    init(User defaultUser);
	public List<User>     list();
	public Optional<User> load(String username, String password);
	public UserService    save(User user);
}