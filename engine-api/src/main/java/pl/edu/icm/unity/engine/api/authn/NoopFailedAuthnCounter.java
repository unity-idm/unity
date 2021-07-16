/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.authn;

public class NoopFailedAuthnCounter implements UnsuccessfulAuthenticationCounter
{
	public static final NoopFailedAuthnCounter INSTANCE = new NoopFailedAuthnCounter(); 
	
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
	public synchronized void successfulAttempt(String ip)
	{
	}
}
