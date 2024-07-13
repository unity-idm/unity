/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.auth.server;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.authn.UnsuccessfulAccessCounterBase;

class RegularInvalidRequestsCounter extends UnsuccessfulAccessCounterBase
{
	public RegularInvalidRequestsCounter(int maxAttepts, long blockTime)
	{
		super(Log.getLogger(Log.U_SERVER_AUTHN, RegularInvalidRequestsCounter.class), maxAttepts, blockTime);
	}
}
