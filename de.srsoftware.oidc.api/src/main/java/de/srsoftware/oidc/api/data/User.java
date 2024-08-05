/* © SRSoftware 2024 */
package de.srsoftware.oidc.api.data;

import java.util.*;
import org.json.JSONObject;

public final class User {
	public static final String EMAIL       = "email";
	public static final String PASSWORD    = "password";
	public static final String PERMISSIONS = "permissions";
	public static final String REALNAME    = "realname";
	public static final String USERNAME    = "username";
	public static final String UUID	       = "uuid";

	private final Set<Permission> permissions = new HashSet<>();

	private String email, hashedPassword, realName, uuid, username;

	public User(String username, String hashedPassword, String realName, String email, String uuid) {
		this.username	    = username;
		this.realName	    = realName;
		this.email	    = email;
		this.hashedPassword = hashedPassword;
		this.uuid	    = uuid;
	}

	public User add(Permission... newPermissions) {
		for (var permission : newPermissions) permissions.add(permission);
		return this;
	}

	public String email() {
		return email;
	}

	public User email(String newVal) {
		email = newVal;
		return this;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) return true;
		if (obj == null || obj.getClass() != this.getClass()) return false;
		var that = (User)obj;
		return Objects.equals(this.uuid, that.uuid);
	}

	public boolean hasPermission(Permission permission) {
		return permissions.contains(permission);
	}

	public String hashedPassword() {
		return hashedPassword;
	}

	public User hashedPassword(String newValue) {
		hashedPassword = newValue;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(username, realName, email, hashedPassword, uuid);
	}


	public Map<String, Object> map(boolean includePassword) {
		return includePassword ? Map.of(USERNAME, username, REALNAME, realName, EMAIL, email, PERMISSIONS, permissions, UUID, uuid, PASSWORD, hashedPassword) : Map.of(USERNAME, username, REALNAME, realName, EMAIL, email, PERMISSIONS, permissions, UUID, uuid);
	}

	public static Optional<User> of(JSONObject json, String userId) {
		var user = new User(json.getString(USERNAME), json.getString(PASSWORD), json.getString(REALNAME), json.getString(EMAIL), userId);

		var perms = json.has(PERMISSIONS) ? json.getJSONArray(PERMISSIONS) : Set.of();
		for (Object perm : perms) {
			try {
				if (perm instanceof String s) perm = Permission.valueOf(s);
				if (perm instanceof Permission p) user.add(p);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return Optional.of(user);
	}

	public String realName() {
		return realName;
	}

	public User realName(String newValue) {
		realName = newValue;
		return this;
	}

	@Override
	public String toString() {
		return "User["
		    + "username=" + username + ", "
		    + "realName=" + realName + ", "
		    + "email=" + email + ", "
		    + "uuid=" + uuid + ']';
	}

	public String username() {
		return username;
	}

	public User username(String newVal) {
		username = newVal;
		return this;
	}


	public String uuid() {
		return uuid;
	}
}
