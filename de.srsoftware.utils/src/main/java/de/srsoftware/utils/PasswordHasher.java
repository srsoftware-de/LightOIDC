/* © SRSoftware 2024 */
package de.srsoftware.utils;

public interface PasswordHasher<T> {
	public String hash(String password, String salt);
	public String salt(String hashedPassword);

	public default boolean matches(String plaintextPassword, String hashedPassword) {
		return hash(plaintextPassword, salt(hashedPassword)).equals(hashedPassword);
	}
}
