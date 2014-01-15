/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.sp.web;

import java.io.IOException;

import org.apache.log4j.Logger;

import pl.edu.icm.unity.saml.sp.HttpPostBindingSupport;
import pl.edu.icm.unity.saml.sp.HttpRedirectBindingSupport;
import pl.edu.icm.unity.saml.sp.RemoteAuthnContext;
import pl.edu.icm.unity.saml.sp.SAMLMessageType;
import pl.edu.icm.unity.saml.sp.SAMLSPProperties.Binding;
import pl.edu.icm.unity.server.utils.Log;

import com.vaadin.server.RequestHandler;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinResponse;
import com.vaadin.server.VaadinServletResponse;
import com.vaadin.server.VaadinSession;
import com.vaadin.server.WrappedSession;

/**
 * Custom Vaadin {@link RequestHandler} which is used to produce a proper GET response to the browser, 
 * redirecting it to IdP. It supports both HTTP POST and HTTP Redirect bindings.
 * 
 * @author K. Benedyczak
 */
public class RedirectRequestHandler implements RequestHandler
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_SAML, RedirectRequestHandler.class);
	public static final String PATH = "/redirectToIdP";
	
	@Override
	public boolean handleRequest(VaadinSession vaadinSession, VaadinRequest request,
			VaadinResponse response) throws IOException
	{
		if (!PATH.equals(request.getPathInfo()))
			return false;
		
		WrappedSession session = vaadinSession.getSession();
		RemoteAuthnContext context = (RemoteAuthnContext) session.getAttribute(
				SAMLRetrieval.REMOTE_AUTHN_CONTEXT);
		if (context == null)
		{
			log.warn("Got a request to the ..." + PATH + " path, " +
					"but no SAML authn context is present in the session.");
			return false;
		}
			

		Binding binding = context.getRequestBinding();
		if (binding == Binding.HTTP_POST)
		{
			handlePost(context, response);
			return true;
		} else if (binding == Binding.HTTP_REDIRECT)
		{
			handleRedirect(context, response);
			return true;
		} else
			return false;
	}
	
	private void handlePost(RemoteAuthnContext context, VaadinResponse response) throws IOException
	{
		response.setContentType("text/html; charset=utf-8");
		response.setHeader("Cache-Control","no-cache,no-store,must-revalidate");
		response.setHeader("Pragma","no-cache");
		response.setDateHeader("Expires", -1);

		log.debug("Starting SAML HTTP POST binding exchange with IdP " + context.getIdpUrl());
		String htmlResponse = HttpPostBindingSupport.getHtmlPOSTFormContents(
				SAMLMessageType.SAMLRequest, context.getIdpUrl(), context.getRequest(), null);
		if (log.isTraceEnabled())
		{
			log.trace("SAML request is:\n" + context.getRequest());
			log.trace("Returned POST form is:\n" + htmlResponse);
		}
		response.getWriter().append(htmlResponse);
	}
	
	private void handleRedirect(RemoteAuthnContext context, VaadinResponse response) throws IOException
	{
		VaadinServletResponse rr = (VaadinServletResponse) response;
		response.setHeader("Cache-Control","no-cache,no-store");
		response.setHeader("Pragma","no-cache");
		log.debug("Starting SAML HTTP Redirect binding exchange with IdP " + context.getIdpUrl());
		String redirectURL = HttpRedirectBindingSupport.getRedirectURL(SAMLMessageType.SAMLRequest, null, 
				context.getRequest(), context.getIdpUrl());
		if (log.isTraceEnabled())
		{
			log.trace("SAML request is:\n" + context.getRequest());
			log.trace("Returned Redirect URL is:\n" + redirectURL);
		}
		rr.sendRedirect(redirectURL);
	}
}
