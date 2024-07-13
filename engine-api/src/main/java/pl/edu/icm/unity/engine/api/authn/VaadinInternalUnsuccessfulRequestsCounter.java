/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.authn;

import pl.edu.icm.unity.base.utils.Log;

public class VaadinInternalUnsuccessfulRequestsCounter extends UnsuccessfulAccessCounterBase
{
	public VaadinInternalUnsuccessfulRequestsCounter(int maxAttepts, long blockTime)
	{
		super(Log.getLogger(Log.U_SERVER_AUTHN, VaadinInternalUnsuccessfulRequestsCounter.class), maxAttepts, blockTime);
	}
}
