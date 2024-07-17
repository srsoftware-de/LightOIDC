/* © SRSoftware 2024 */
package de.srsoftware.oidc.datastore.file;

import static java.nio.charset.StandardCharsets.UTF_8;

import de.srsoftware.oidc.api.PasswordHasher;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;


public class UuidHasher implements PasswordHasher<String> {
	private static final String SHA256 = "SHA-256";

	private final MessageDigest digest;

	public UuidHasher() throws NoSuchAlgorithmException {
		digest = MessageDigest.getInstance(SHA256);
	}

	@Override
	public String hash(String password, String uuid) {
		var salt       = uuid;
		var saltedPass = "%s %s".formatted(salt, password);
		var bytes      = digest.digest(saltedPass.getBytes(UTF_8));

		return "%s@%s".formatted(hex(bytes), salt);
	}

	@Override
	public String salt(String hashedPassword) {
		return hashedPassword.split("@")[1];
	}

	public static String hex(byte[] bytes) {
		StringBuilder sb = new StringBuilder(bytes.length * 2);
		for (byte b : bytes) sb.append(String.format("%02x", b));
		return sb.toString();
	}
}
