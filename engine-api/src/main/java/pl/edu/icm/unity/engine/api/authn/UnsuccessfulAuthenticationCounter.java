/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.authn;

/**
 * Counts unsuccessful authentication attempts per client's IP address.
 */
public interface UnsuccessfulAuthenticationCounter
{
	long getRemainingBlockedTime(String ip);
	void unsuccessfulAttempt(String ip);
	void successfulAttempt(String ip);
}
