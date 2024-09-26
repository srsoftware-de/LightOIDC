package de.srsoftware.oidc.datastore.encrypted;

import de.srsoftware.oidc.api.UserService;
import de.srsoftware.oidc.api.data.AccessToken;
import de.srsoftware.oidc.api.data.User;
import de.srsoftware.utils.Optionals;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class EncryptedUserService extends EncryptedConfig implements UserService {
	private final UserService backend;

	EncryptedUserService(UserService backend, String key, String salt){
		super(key,salt);
		this.backend = backend;
	}

	@Override
	public AccessToken accessToken(User user) {
		return backend.accessToken(encrypt(user));
	}

	@Override
	public Optional<User> consumeToken(String accessToken) {
		return backend.consumeToken(accessToken).map(this::decrypt);
	}

	public User decrypt(User secret){
return secret;
	}

	@Override
	public UserService delete(User user) {
		backend.delete(encrypt(user));
		return this;
	}

	public User encrypt(User plain){
		return plain;
	}

	@Override
	public Optional<User> forToken(String accessToken) {
		return backend.forToken(accessToken).map(this::decrypt);
	}

	@Override
	public UserService init(User defaultUser) {
		backend.init(encrypt(defaultUser));
		return this;
	}

	@Override
	public List<User> list() {
		return backend.list().stream().map(this::decrypt).toList();
	}

	@Override
	public Set<User> find(String idOrEmail) {
		return backend.find(idOrEmail).stream().map(this::decrypt).collect(Collectors.toSet());
	}

	@Override
	public Optional<User> load(String id) {
		return backend.load(id).map(this::decrypt);
	}

	@Override
	public Optional<User> load(String username, String password) {
		return backend.load(encrypt(username),encrypt(password));
	}

	@Override
	public boolean passwordMatches(String plaintextPassword, User user) {
		return backend.passwordMatches(encrypt(plaintextPassword),encrypt(user));
	}

	@Override
	public EncryptedUserService save(User user) {
		backend.save(encrypt(user));
		return this;
	}

	@Override
	public EncryptedUserService updatePassword(User user, String plaintextPassword) {
		backend.updatePassword(encrypt(user),encrypt(plaintextPassword));
		return this;
	}
}
