/*
 * Copyright (c) 2017 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.sp.web;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.Logger;

import eu.unicore.samly2.binding.HttpPostBindingSupport;
import eu.unicore.samly2.binding.HttpRedirectBindingSupport;
import eu.unicore.samly2.binding.SAMLMessageType;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.saml.SamlProperties.Binding;
import pl.edu.icm.unity.saml.sp.RemoteAuthnContext;

/**
 * Final redirection code. Universal, can be used from both UI/vaadin and filter.
 * 
 * @author K. Benedyczak
 */
class RedirectRequestHandler
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_SAML,
			RedirectRequestHandler.class);
	
	static boolean handleRequest(RemoteAuthnContext context, HttpServletResponse response) throws IOException
	{
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
	
	private static void handlePost(RemoteAuthnContext context, HttpServletResponse response) throws IOException
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
	
	private static void handleRedirect(RemoteAuthnContext context, HttpServletResponse response) throws IOException
	{
		setCommonHeaders(response);
		log.debug("Starting SAML HTTP Redirect binding exchange with IdP " + context.getIdpUrl());
		String redirectURL = HttpRedirectBindingSupport.getRedirectURL(SAMLMessageType.SAMLRequest, 
				context.getRelayState(), context.getRequest(), context.getIdpUrl());
		if (log.isTraceEnabled())
		{
			log.trace("SAML request is:\n" + context.getRequest());
			log.trace("Returned Redirect URL is:\n" + redirectURL);
		}
		response.sendRedirect(redirectURL);
	}
	
	private static void setCommonHeaders(HttpServletResponse response)
	{
		response.setHeader("Cache-Control","no-cache,no-store");
		response.setHeader("Pragma","no-cache");
	}
}
