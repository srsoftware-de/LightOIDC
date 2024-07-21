package de.srsoftware.oidc.api;

import java.util.Set;

public record Client(String id, String name, String secret, Set<String> redirectUris) {
}
