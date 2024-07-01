/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.authn;

public class NoOpLoginCounter implements UnsuccessfulAccessCounter
{
	@Override
	public long getRemainingBlockedTime(String ip)
	{
		return 0;
	}

	@Override
	public void unsuccessfulAttempt(String ip)
	{
	}

	@Override
	public void successfulAttempt(String ip)
	{
	}
}
