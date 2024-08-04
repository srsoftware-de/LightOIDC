/* © SRSoftware 2024 */
package de.srsoftware.oidc.api.data;

public record Authorization(String clientId, String userId, AuthorizedScopes scopes) {
}