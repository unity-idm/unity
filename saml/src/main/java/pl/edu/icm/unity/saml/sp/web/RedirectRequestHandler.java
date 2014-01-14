/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.sp.web;

import java.io.IOException;

import org.apache.xmlbeans.impl.util.Base64;

import pl.edu.icm.unity.saml.sp.HttpRedirectBindingSupport;
import pl.edu.icm.unity.saml.sp.HttpRedirectBindingSupport.MessageType;
import pl.edu.icm.unity.saml.sp.web.SAMLSPRetrievalProperties.Binding;

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
			return false;
		//TODO logging
		Binding binding = context.getBinding();
		if (binding == Binding.httpPost)
		{
			handlePost(context, response);
			return true;
		} else if (binding == Binding.httpRedirect)
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
		
		String htmlResponse = getHtmlPOSTFormContents(context.getIdpUrl(), context.getRequest(), null);
		response.getWriter().append(htmlResponse);
	}
	
	private void handleRedirect(RemoteAuthnContext context, VaadinResponse response) throws IOException
	{
		VaadinServletResponse rr = (VaadinServletResponse) response;
		response.setHeader("Cache-Control","no-cache,no-store");
		response.setHeader("Pragma","no-cache");
		String redirectURL = HttpRedirectBindingSupport.getRedirectURL(MessageType.SAMLRequest, null, 
				context.getRequest(), context.getIdpUrl());
		rr.sendRedirect(redirectURL);
	}
	
	
	private String getHtmlPOSTFormContents(String identityProviderURL, 
			String xmlRequest, String relayState)
	{
		String f = formForm.replace("__ACTION__", identityProviderURL);
		f = f.replace("__RELAYSTATE__", relayState == null ? "" : relayState);
		String encodedReq = new String(Base64.encode(xmlRequest.getBytes()));
		f = f.replace("__SAMLREQUEST__", encodedReq);
		return f;
	}	

	private static final String formForm = 
		"<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
		"<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.1//EN\" \"http://www.w3.org/TR/xhtml11/DTD/xhtml11.dtd\">" +
		"<html xmlns=\"http://www.w3.org/1999/xhtml\" xml:lang=\"en\">" +
		"<body onload=\"document.forms[0].submit()\">" +
		"<noscript>" +
		"<p>" +
		"<strong>Note:</strong> Since your browser does not support JavaScript," +
		"you must press the Continue button once to proceed." +
		"</p>" +
		"</noscript>" +
		"<form action=\"__ACTION__\" method=\"post\">" +
		"<div>" +
		"<input type=\"hidden\" name=\"RelayState\" value=\"__RELAYSTATE__\"/>" +
		"<input type=\"hidden\" name=\"SAMLRequest\" value=\"__SAMLREQUEST__\"/>" +
		"</div>" +
		"<noscript>" +
		"<div>" +
		"<input type=\"submit\" value=\"Continue\"/>" +
		"</div>" +
		"</noscript>" +
		"</form>" +
		"</body></html>";
}
