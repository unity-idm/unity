/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.authn;

public interface UnsuccessfulAuthenticationCounter
{
	long getRemainingBlockedTime(String ip);

	void unsuccessfulAttempt(String ip);

	void successfulAttempt(String ip);

}