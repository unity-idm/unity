/*
 * Copyright (c) 2017 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.sp.web;

import java.io.IOException;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.logging.log4j.Logger;

import com.vaadin.server.VaadinServletResponse;

import eu.unicore.samly2.binding.HttpPostBindingSupport;
import eu.unicore.samly2.binding.HttpRedirectBindingSupport;
import eu.unicore.samly2.binding.SAMLMessageType;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.saml.SamlProperties.Binding;
import pl.edu.icm.unity.saml.sp.RemoteAuthnContext;
import pl.edu.icm.unity.saml.sp.SAMLExchange;
import pl.edu.icm.unity.saml.sp.SAMLSPProperties;
import pl.edu.icm.unity.saml.sp.SamlContextManagement;
import pl.edu.icm.unity.webui.authn.AuthenticationOptionKeyUtils;
import pl.edu.icm.unity.webui.authn.AuthenticationUI;
import pl.edu.icm.unity.webui.authn.ProxyAuthenticationFilter;

/**
 * Support for automatic proxy authentication. Automatically redirects to external IdP.
 * 
 * @author K. Benedyczak
 */
class SAMLProxyAuthnHandler
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_SAML,
			SAMLProxyAuthnHandler.class);
	
	private final SAMLExchange credentialExchange;
	private final SamlContextManagement samlContextManagement;

	SAMLProxyAuthnHandler(SAMLExchange credentialExchange, SamlContextManagement samlContextManagement)
	{
		this.credentialExchange = credentialExchange;
		this.samlContextManagement = samlContextManagement;
	}
	
	boolean triggerAutomatedAuthentication(HttpServletRequest httpRequest,
			HttpServletResponse httpResponse) throws IOException
	{
		String idpKey = getIdpConfigKey(httpRequest);
		startLogin(idpKey, httpRequest, httpResponse);
		return true;
	}

	private String getIdpConfigKey(HttpServletRequest httpRequest)
	{
		String requestedIdP = httpRequest.getParameter(AuthenticationUI.IDP_SELECT_PARAM);
		SAMLSPProperties clientProperties = credentialExchange.getSamlValidatorSettings();
		Set<String> keys = clientProperties.getStructuredListKeys(SAMLSPProperties.IDP_PREFIX);
		
		if (requestedIdP == null)
		{
			if (keys.size() > 1)
				throw new IllegalStateException("SAML authentication option was not requested with " 
					+ AuthenticationUI.IDP_SELECT_PARAM
					+ " and there are multiple options installed: "
					+ "can not perform automatic authentication.");
			return keys.iterator().next();
		}
		
		String authnOption = SAMLSPProperties.IDP_PREFIX + 
				AuthenticationOptionKeyUtils.decodeOption(requestedIdP) + ".";
		if (!keys.contains(authnOption))
			throw new IllegalStateException("Client requested authN option " + authnOption 
					+", which is not available in "
					+ "the authenticator selected for automated proxy authN. "
					+ "Ignoring the request.");
		return authnOption;
	}
	
	private void startLogin(String idpConfigKey, HttpServletRequest httpRequest,
			HttpServletResponse httpResponse) throws IOException
	{
		HttpSession session = httpRequest.getSession();
		RemoteAuthnContext context = (RemoteAuthnContext) session.getAttribute(
				SAMLRetrieval.REMOTE_AUTHN_CONTEXT);
		if (context != null)
		{
			log.warn("Starting a new external SAML authentication, killing the previous "
					+ "one which is still in progress.");
		}
		
		String currentRelativeURI = ProxyAuthenticationFilter.getCurrentRelativeURL(httpRequest);

		try
		{
			context = credentialExchange.createSAMLRequest(idpConfigKey, currentRelativeURI);
			session.setAttribute(SAMLRetrieval.REMOTE_AUTHN_CONTEXT, context);
			samlContextManagement.addAuthnContext(context);
		} catch (Exception e)
		{
			throw new IllegalStateException("Can not create SAML authN request", e);
		}
		handleRequestInternal(context, httpResponse);
	}
	
	//FIXME huge code duplication with RedirectRequestHandler
	private boolean handleRequestInternal(RemoteAuthnContext context, 
			HttpServletResponse response) throws IOException
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
	
	private void handlePost(RemoteAuthnContext context, HttpServletResponse response) throws IOException
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
	
	private void handleRedirect(RemoteAuthnContext context, HttpServletResponse response) throws IOException
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
	
	private void setCommonHeaders(HttpServletResponse response)
	{
		response.setHeader("Cache-Control","no-cache,no-store");
		response.setHeader("Pragma","no-cache");
	}
}
