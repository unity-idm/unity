/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.authn;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.server.api.internal.LoginSession;
import pl.edu.icm.unity.server.authn.LogoutProcessor;
import pl.edu.icm.unity.server.authn.LogoutProcessorFactory;

/**
 * Uses available {@link LogoutProcessorFactory}ies to produce logout processors and forwards requests to them
 * 
 * @author K. Benedyczak
 */
@Component
public class LogoutProcessorsManager
{
	private List<LogoutProcessor> processors;
	
	@Autowired(required=false)
	public LogoutProcessorsManager(List<LogoutProcessorFactory> factories)
	{
		processors = new ArrayList<>(factories.size());
		factories.forEach(f -> processors.add(f.getInstance()));
	}

	public LogoutProcessorsManager()
	{
		processors = new ArrayList<>(0);
	}
	
	public void handleAsyncLogout(LoginSession session, String requestersRelayState, String returnUrl, 
			HttpServletResponse response) throws IOException
	{
		for (LogoutProcessor logoutProcessor: processors)
			logoutProcessor.handleAsyncLogout(session, requestersRelayState, returnUrl, response);
	}
	
	public boolean handleSynchronousLogout(LoginSession session)
	{
		boolean ret = true;
		for (LogoutProcessor logoutProcessor: processors)
			ret &= logoutProcessor.handleSynchronousLogout(session);
		return ret;
	}
}
