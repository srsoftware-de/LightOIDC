/* © SRSoftware 2024 */
package de.srsoftware.oidc.api;

public record Authorization(String clientId, String userId, AuthorizedScopes scopes) {
}