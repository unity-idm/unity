/*
 * Copyright (c) 2017 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.sp.web;

import java.io.IOException;
import java.util.Set;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.logging.log4j.Logger;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.saml.sp.RemoteAuthnContext;
import pl.edu.icm.unity.saml.sp.SAMLExchange;
import pl.edu.icm.unity.saml.sp.SAMLSPProperties;
import pl.edu.icm.unity.saml.sp.SamlContextManagement;
import pl.edu.icm.unity.types.authn.AuthenticationOptionKeyUtils;
import pl.edu.icm.unity.webui.authn.PreferredAuthenticationHelper;
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
	private final String authenticatorId;

	SAMLProxyAuthnHandler(SAMLExchange credentialExchange, SamlContextManagement samlContextManagement, 
			String authenticatorId)
	{
		this.credentialExchange = credentialExchange;
		this.samlContextManagement = samlContextManagement;
		this.authenticatorId = authenticatorId;
	}
	
	boolean triggerAutomatedAuthentication(HttpServletRequest httpRequest,
			HttpServletResponse httpResponse, String endpointPath) throws IOException
	{
		String idpKey = getIdpConfigKey(httpRequest);
		return startLogin(idpKey, httpRequest, httpResponse, endpointPath);
	}

	private String getIdpConfigKey(HttpServletRequest httpRequest)
	{
		String requestedIdP = httpRequest.getParameter(PreferredAuthenticationHelper.IDP_SELECT_PARAM);
		SAMLSPProperties clientProperties = credentialExchange.getSamlValidatorSettings();
		Set<String> keys = clientProperties.getStructuredListKeys(SAMLSPProperties.IDP_PREFIX);
		
		if (requestedIdP == null)
		{
			if (keys.size() > 1)
				throw new IllegalStateException("SAML authentication option was not requested with " 
					+ PreferredAuthenticationHelper.IDP_SELECT_PARAM
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
	
	private boolean startLogin(String idpConfigKey, HttpServletRequest httpRequest,
			HttpServletResponse httpResponse, String endpointPath) throws IOException
	{
		HttpSession session = httpRequest.getSession();
		RemoteAuthnContext context = (RemoteAuthnContext) session.getAttribute(
				SAMLRetrieval.REMOTE_AUTHN_CONTEXT);
		if (context != null)
		{
			log.debug("Ignoring automated login as the previous remote SAML authentication "
					+ "is still in progress.");
			return false;
		}
		
		String currentRelativeURI = ProxyAuthenticationFilter.getCurrentRelativeURL(httpRequest);
		log.debug("Starting automatic proxy authentication with remote SAML IdP "
				+ "configured under {}, current relative URI is {}", idpConfigKey, currentRelativeURI);	

		try
		{
			context = credentialExchange.createSAMLRequest(idpConfigKey, currentRelativeURI);
			session.setAttribute(SAMLRetrieval.REMOTE_AUTHN_CONTEXT, context);
			session.setAttribute(ProxyAuthenticationFilter.AUTOMATED_LOGIN_FIRED, "true");
			samlContextManagement.addAuthnContext(context);
		} catch (Exception e)
		{
			throw new IllegalStateException("Can not create SAML authN request", e);
		}
		
		setLastIdpCookie(httpResponse, idpConfigKey, endpointPath);
		
		RedirectRequestHandler.handleRequest(context, httpResponse);
		return true;
	}
	
	private void setLastIdpCookie(HttpServletResponse httpResponse, String idpConfigKey, String endpointPath)
	{
		String optionId = idpConfigKey.substring(SAMLSPProperties.IDP_PREFIX.length(), idpConfigKey.length()-1);
		String selectedAuthn = AuthenticationOptionKeyUtils.encode(authenticatorId, optionId);
		Cookie lastIdpCookie = PreferredAuthenticationHelper.createLastIdpCookie(
				endpointPath, selectedAuthn);
		httpResponse.addCookie(lastIdpCookie);
	}
}
