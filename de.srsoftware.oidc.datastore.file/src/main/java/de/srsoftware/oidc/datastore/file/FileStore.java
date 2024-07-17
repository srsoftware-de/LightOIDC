/* © SRSoftware 2024 */
package de.srsoftware.oidc.datastore.file; /* © SRSoftware 2024 */
import static de.srsoftware.oidc.api.User.*;

import de.srsoftware.oidc.api.PasswordHasher;
import de.srsoftware.oidc.api.User;
import de.srsoftware.oidc.api.UserService;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import org.json.JSONObject;

public class FileStore implements UserService {
	private static final String USERS = "users";

	private final Path       storageFile;
	private final JSONObject json;
	private final PasswordHasher<String> passwordHasher;

	public FileStore(File storage, PasswordHasher<String> passwordHasher) throws IOException {
		this.storageFile    = storage.toPath();
		this.passwordHasher = passwordHasher;

		if (!storage.exists()) {
			var parent = storage.getParentFile();
			if (!parent.exists() && !parent.mkdirs()) throw new FileNotFoundException("Failed to create directory %s".formatted(parent));
			Files.writeString(storageFile, "{}");
		}
		json = new JSONObject(Files.readString(storageFile));
	}

	@Override
	public Optional<User> load(String username, String password) {
		try {
			var users = json.getJSONObject(USERS);
			var uuids = users.keySet();
			for (String uuid : uuids) {
				var user = users.getJSONObject(uuid);
				if (!user.getString(USERNAME).equals(username)) continue;
				var hashedPass = user.getString(PASSWORD);
				if (passwordHasher.matches(password, hashedPass)) {
					return Optional.of(new User(username, hashedPass, user.getString(REALNAME), user.getString(EMAIL), uuid));
				}
			}
			return Optional.empty();
		} catch (Exception e) {
			return Optional.empty();
		}
	}

	@Override
	public UserService delete(User user) {
		return null;
	}

	@Override
	public UserService init(User defaultUser) {
		if (!json.has(USERS)) save(defaultUser);
		return this;
	}

	@Override
	public UserService save(User user) {
		JSONObject users;
		if (!json.has(USERS)) {
			json.put(USERS, users = new JSONObject());
		} else
			users = json.getJSONObject(USERS);
		users.put(user.uuid(), user.map(true));
		try {
			Files.writeString(storageFile, json.toString(2));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return this;
	}


	@Override
	public List<User> list() {
		return List.of();
	}
}
