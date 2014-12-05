/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.sp.web;

import java.io.IOException;

import org.apache.log4j.Logger;

import pl.edu.icm.unity.saml.SamlProperties.Binding;
import pl.edu.icm.unity.saml.sp.RemoteAuthnContext;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.webui.authn.remote.AbstractRedirectRequestHandler;

import com.vaadin.server.RequestHandler;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinResponse;
import com.vaadin.server.VaadinServletResponse;
import com.vaadin.server.VaadinSession;

import eu.unicore.samly2.binding.HttpPostBindingSupport;
import eu.unicore.samly2.binding.HttpRedirectBindingSupport;
import eu.unicore.samly2.binding.SAMLMessageType;

/**
 * Custom Vaadin {@link RequestHandler} which is used to produce a proper GET response to the browser, 
 * redirecting it to IdP. It supports both HTTP POST and HTTP Redirect bindings.
 * 
 * @author K. Benedyczak
 */
public class RedirectRequestHandler extends AbstractRedirectRequestHandler
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_SAML, RedirectRequestHandler.class);
	
	public RedirectRequestHandler()
	{
		super(SAMLRetrieval.REMOTE_AUTHN_CONTEXT);
	}
	
	@Override
	protected boolean handleRequestInternal(Object contextO, VaadinSession vaadinSession,
			VaadinRequest request, VaadinResponse response) throws IOException
	{
		RemoteAuthnContext context = (RemoteAuthnContext)contextO;
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
		setCommonHeaders(response);
		response.setDateHeader("Expires", -1);

		log.debug("Starting SAML HTTP POST binding exchange with IdP " + context.getIdpUrl());
		String htmlResponse = HttpPostBindingSupport.getHtmlPOSTFormContents(
				SAMLMessageType.SAMLRequest, context.getIdpUrl(), context.getRequest(), 
				context.getRelayState());
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
		setCommonHeaders(response);
		log.debug("Starting SAML HTTP Redirect binding exchange with IdP " + context.getIdpUrl());
		String redirectURL = HttpRedirectBindingSupport.getRedirectURL(SAMLMessageType.SAMLRequest, 
				context.getRelayState(), context.getRequest(), context.getIdpUrl());
		if (log.isTraceEnabled())
		{
			log.trace("SAML request is:\n" + context.getRequest());
			log.trace("Returned Redirect URL is:\n" + redirectURL);
		}
		rr.sendRedirect(redirectURL);
	}
}
