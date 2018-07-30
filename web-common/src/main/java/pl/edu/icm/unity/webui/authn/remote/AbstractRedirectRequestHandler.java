/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.authn.remote;

import java.io.IOException;
import java.util.UUID;

import org.apache.logging.log4j.Logger;

import com.vaadin.server.RequestHandler;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinResponse;
import com.vaadin.server.VaadinSession;
import com.vaadin.server.WrappedSession;

import pl.edu.icm.unity.base.utils.Log;


/**
 * Custom Vaadin {@link RequestHandler} which is used to produce a proper response to the browser, 
 * redirecting it to IdP. This class should be extended with protocol specific implementation. 
 * 
 * @author K. Benedyczak
 */
public abstract class AbstractRedirectRequestHandler implements RequestHandler
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, AbstractRedirectRequestHandler.class);
	public static final String TRIGGERING_PARAMETER = "redirectToIdP";
	private String uniqueId;
	private String contextKey;
	
	public AbstractRedirectRequestHandler(String contextKey)
	{
		this.uniqueId = UUID.randomUUID().toString();
		this.contextKey = contextKey;
	}

	@Override
	public boolean handleRequest(VaadinSession vaadinSession, VaadinRequest request,
			VaadinResponse response) throws IOException
	{
		String fire = request.getParameter(TRIGGERING_PARAMETER); 
		if (fire == null || !fire.equals(uniqueId))
			return false;
				
		WrappedSession session = vaadinSession.getSession();
		Object context = session.getAttribute(contextKey);
		if (context == null)
		{
			log.warn("Got a request for outgoing remote authentication redirection, " +
					"but no authn context is present in the session.");
			return false;
		}
		return handleRequestInternal(context, vaadinSession, request, response);
	}
	
	protected abstract boolean handleRequestInternal(Object context, VaadinSession vaadinSession, 
			VaadinRequest request, VaadinResponse response) throws IOException;
	
	public String getTriggeringParam()
	{
		return AbstractRedirectRequestHandler.TRIGGERING_PARAMETER + "=" + uniqueId;
	}
	
	protected void setCommonHeaders(VaadinResponse response)
	{
		response.setHeader("Cache-Control","no-cache,no-store");
		response.setHeader("Pragma","no-cache");
	}
}
