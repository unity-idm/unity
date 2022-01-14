/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.rest.authn;

import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.log4j.MDC;

public class LogContextCleaningInterceptor extends AbstractPhaseInterceptor<Message>
{
	public LogContextCleaningInterceptor()
	{
		super(Phase.POST_INVOKE);
	}

	@Override
	public void handleMessage(Message message) throws Fault
	{
		clearMDC();
	}

	private void clearMDC()
	{
		MDC.clear();
	}
}
