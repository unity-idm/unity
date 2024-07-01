/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.authn;

import pl.edu.icm.unity.base.utils.Log;

/**
 * Counts unsuccessful authentication attempts per client's IP address.
 *
 * @author K. Benedyczak
 */
public class DefaultUnsuccessfulAuthenticationCounter extends UnsuccessfulAccessCounterBase
{
	public DefaultUnsuccessfulAuthenticationCounter(int maxAttepts, long blockTime)
	{
		super(Log.getLogger(Log.U_SERVER_AUTHN, DefaultUnsuccessfulAuthenticationCounter.class), maxAttepts, blockTime);
	}
}
