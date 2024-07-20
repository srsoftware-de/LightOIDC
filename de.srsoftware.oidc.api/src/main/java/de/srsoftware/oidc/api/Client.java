package de.srsoftware.oidc.api;

import java.util.Set;

public record Client(String id, Set<String> redirectUris) {
}
