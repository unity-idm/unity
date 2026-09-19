/*
 * Copyright (c) 2026 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as.devicesignin;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class DeviceCodeVerificationThrottleTest
{
	@Test
	void shouldNotBlockBeforeMaxAttemptsReached()
	{
		DeviceCodeVerificationThrottle tested = new DeviceCodeVerificationThrottle();

		for (int i = 0; i < 4; i++)
			tested.unsuccessfulAttempt(100L);

		assertThat(tested.getRemainingBlockedTimeMs(100L)).isEqualTo(0);
	}

	@Test
	void shouldBlockAfterMaxAttemptsReached()
	{
		DeviceCodeVerificationThrottle tested = new DeviceCodeVerificationThrottle();

		for (int i = 0; i < 5; i++)
			tested.unsuccessfulAttempt(100L);

		assertThat(tested.getRemainingBlockedTimeMs(100L)).isGreaterThan(0);
	}

	@Test
	void shouldNotBlockUnrelatedEntity()
	{
		DeviceCodeVerificationThrottle tested = new DeviceCodeVerificationThrottle();

		for (int i = 0; i < 5; i++)
			tested.unsuccessfulAttempt(100L);

		assertThat(tested.getRemainingBlockedTimeMs(200L)).isEqualTo(0);
	}

	@Test
	void successfulAttemptClearsPriorFailures()
	{
		DeviceCodeVerificationThrottle tested = new DeviceCodeVerificationThrottle();

		for (int i = 0; i < 4; i++)
			tested.unsuccessfulAttempt(100L);
		tested.successfulAttempt(100L);
		tested.unsuccessfulAttempt(100L);

		// only 1 failure since the reset - nowhere near the 5-attempt threshold
		assertThat(tested.getRemainingBlockedTimeMs(100L)).isEqualTo(0);
	}
}
