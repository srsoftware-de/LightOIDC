/* © SRSoftware 2024 */
package de.srsoftware.oidc.api.data;

import java.time.Instant;

public class FailedLogin {
	private final String userId;
	private int	     attempts;
	private Instant	     releaseTime;

	public FailedLogin(String userId) {
		this.userId   = userId;
		this.attempts = 0;
		count();
	}

	public void count() {
		attempts++;
		if (attempts > 13) attempts = 13;
		var seconds = 1;
		for (long i = 0; i < attempts; i++) seconds *= 2;
		releaseTime = Instant.now().plusSeconds(seconds);
	}

	public int attempts() {
		return attempts;
	}

	public Instant releaseTime() {
		return releaseTime;
	}
}
