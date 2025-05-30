/* © SRSoftware 2025 */
package de.srsoftware.oidc.api.data;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

public class Lock {
	private int	attempts;
	private Instant releaseTime;

	public Lock() {
		this.attempts = 0;
	}

	public Lock count() {
		attempts++;
		if (attempts > 13) attempts = 13;
		var seconds = 5;
		for (long i = 0; i < attempts; i++) seconds *= 2;
		releaseTime = Instant.now().plusSeconds(seconds).truncatedTo(ChronoUnit.SECONDS);
		return this;
	}

	public int attempts() {
		return attempts;
	}

	public Instant releaseTime() {
		return releaseTime;
	}
}
