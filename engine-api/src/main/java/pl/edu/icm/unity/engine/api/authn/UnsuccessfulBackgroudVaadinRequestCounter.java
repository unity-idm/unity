/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.authn;

import pl.edu.icm.unity.base.utils.Log;

/**
 * Counts unsuccessful Vaadin request per client's IP address.
 *
 * @author K. Benedyczak
 */
public class UnsuccessfulBackgroudVaadinRequestCounter extends UnsuccessfulAccessCounterBase
{
	public UnsuccessfulBackgroudVaadinRequestCounter(int maxAttepts, long blockTime)
	{
		super(Log.getLogger(Log.U_SERVER_AUTHN, UnsuccessfulBackgroudVaadinRequestCounter.class), maxAttepts, blockTime);
	}
}
