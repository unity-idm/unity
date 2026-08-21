/*
 * Copyright (c) 2026 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as.devicesignin;

import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.authn.UnsuccessfulAccessCounterBase;

/**
 * RFC 8628 §5.4 requires the authorization server to rate-limit user_code verification attempts.
 * Device sign-in already requires an authenticated Unity user before the code-entry screen is even
 * reachable, so this throttles per entity id rather than per client IP: the realistic threat here
 * is a logged-in user guessing another session's code, not an anonymous attacker, and per-IP
 * throttling would risk collaterally locking out unrelated users behind a shared NAT/proxy.
 * <p>
 * A dedicated instance/bean (not shared with the login attempt counter), so device code guessing
 * and login lockouts can never interfere with each other.
 */
@Component
class DeviceCodeVerificationThrottle
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_OAUTH, DeviceCodeVerificationThrottle.class);
	private static final int MAX_ATTEMPTS = 5;
	private static final long BLOCK_TIME_MS = 5 * 60 * 1000L;

	private final UnsuccessfulAccessCounterBase counter = new UnsuccessfulAccessCounterBase(log, MAX_ATTEMPTS,
			BLOCK_TIME_MS);

	long getRemainingBlockedTimeMs(long entityId)
	{
		return counter.getRemainingBlockedTime(String.valueOf(entityId));
	}

	void unsuccessfulAttempt(long entityId)
	{
		counter.unsuccessfulAttempt(String.valueOf(entityId));
	}

	void successfulAttempt(long entityId)
	{
		counter.successfulAttempt(String.valueOf(entityId));
	}
}
