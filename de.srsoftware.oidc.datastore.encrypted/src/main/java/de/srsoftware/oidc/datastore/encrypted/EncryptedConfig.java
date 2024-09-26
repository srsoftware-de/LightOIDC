/* © SRSoftware 2024 */
package de.srsoftware.oidc.datastore.encrypted;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Base64;
import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

public class EncryptedConfig {
	private final Cipher        cipher;
	private static final int    KEY_LENGTH      = 256;
	private static final int    ITERATION_COUNT = 65536;
	private final SecretKeySpec secretKeySpec;

	public EncryptedConfig(String key, String salt) {
		try {
			SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
			KeySpec	 spec    = new PBEKeySpec(key.toCharArray(), salt.getBytes(), ITERATION_COUNT, KEY_LENGTH);
			SecretKey	 tmp     = factory.generateSecret(spec);
			secretKeySpec	         = new SecretKeySpec(tmp.getEncoded(), "AES");

			cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
		} catch (NoSuchPaddingException | NoSuchAlgorithmException | InvalidKeySpecException ex) {
			throw new RuntimeException(ex);
		}
	}

	public String encrypt(String plain) {
		if (plain == null) return null;
		SecureRandom secureRandom = new SecureRandom();
		byte[]       iv	          = new byte[16];
		secureRandom.nextBytes(iv);
		IvParameterSpec initVector = new IvParameterSpec(iv);
		try {
			cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, initVector);
			byte[] cipherText    = cipher.doFinal(plain.getBytes(UTF_8));
			byte[] encryptedData = new byte[iv.length + cipherText.length];
			System.arraycopy(iv, 0, encryptedData, 0, iv.length);
			System.arraycopy(cipherText, 0, encryptedData, iv.length, cipherText.length);

			return Base64.getEncoder().encodeToString(encryptedData);

		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public String decrypt(String secret) {
		if (secret == null) return null;
		if (secret.isBlank()) return "";
		byte[] encryptedData = Base64.getDecoder().decode(secret);
		byte[] iv	     = new byte[16];
		System.arraycopy(encryptedData, 0, iv, 0, iv.length);
		IvParameterSpec ivspec = new IvParameterSpec(iv);
		try {
			cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, ivspec);
			byte[] cipherText = new byte[encryptedData.length - 16];
			System.arraycopy(encryptedData, 16, cipherText, 0, cipherText.length);

			byte[] decryptedText = cipher.doFinal(cipherText);
			return new String(decryptedText, UTF_8);

		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
