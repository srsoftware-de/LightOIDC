/* © SRSoftware 2025 */
package de.srsoftware.oidc.datastore.encrypted;

import static java.util.Optional.empty;

import de.srsoftware.oidc.api.ClientService;
import de.srsoftware.oidc.api.data.Client;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class EncryptedClientService extends EncryptedConfig implements ClientService {
	private final ClientService backend;

	public EncryptedClientService(String key, String salt, ClientService backend) {
		super(key, salt);
		this.backend = backend;
	}

	public Client decrypt(Client client) {
		var decryptedUrls = client.redirectUris().stream().map(this::decrypt).collect(Collectors.toSet());
		return new Client(decrypt(client.id()), decrypt(client.name()), decrypt(client.secret()), decryptedUrls).landingPage(decrypt(client.landingPage())).tokenValidity(client.tokenValidity());
	}

	public Client encrypt(Client client) {
		var encryptedUrls = client.redirectUris().stream().map(this::encrypt).collect(Collectors.toSet());
		return new Client(encrypt(client.id()), encrypt(client.name()), encrypt(client.secret()), encryptedUrls).landingPage(encrypt(client.landingPage())).tokenValidity(client.tokenValidity());
	}

	@Override
	public Optional<Client> getClient(String clientId) {
		if (clientId == null || clientId.isBlank()) return empty();
		for (var encrypted : backend.listClients()) {
			var decrypted = decrypt(encrypted);
			if (decrypted.id().equals(clientId)) return Optional.of(decrypted);
		}
		return empty();
	}

	@Override
	public List<Client> listClients() {
		return backend.listClients().stream().map(this::decrypt).toList();
	}

	@Override
	public ClientService remove(String clientId) {
		if (clientId == null || clientId.isBlank()) return this;
		for (var encrypted : backend.listClients()) {
			var decrypted = decrypt(encrypted);
			if (decrypted.id().equals(clientId)) {
				backend.remove(encrypted.id());
				break;
			}
		}
		return this;
	}

	@Override
	public ClientService save(Client client) {
		remove(client.id());
		backend.save(encrypt(client));
		return this;
	}
}
