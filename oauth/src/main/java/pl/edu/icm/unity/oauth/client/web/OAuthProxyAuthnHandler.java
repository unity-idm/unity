/*
 * Copyright (c) 2017 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.client.web;

import java.io.IOException;
import java.util.Optional;
import java.util.Set;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.logging.log4j.Logger;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.oauth.client.OAuthContext;
import pl.edu.icm.unity.oauth.client.OAuthExchange;
import pl.edu.icm.unity.oauth.client.config.OAuthClientProperties;
import pl.edu.icm.unity.types.authn.AuthenticationOptionKeyUtils;
import pl.edu.icm.unity.webui.authn.PreferredAuthenticationHelper;
import pl.edu.icm.unity.webui.authn.ProxyAuthenticationFilter;

/**
 * Support for automatic proxy authentication. Automatically redirects to external AS.
 * 
 * @author K. Benedyczak
 */
class OAuthProxyAuthnHandler
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_OAUTH,
			OAuthProxyAuthnHandler.class);
	
	private final OAuthExchange credentialExchange;
	private final String authenticatorId;
	
	OAuthProxyAuthnHandler(OAuthExchange credentialExchange, String authenticatorId)
	{
		this.credentialExchange = credentialExchange;
		this.authenticatorId = authenticatorId;
	}

	private String getIdpConfigKey(HttpServletRequest httpRequest)
	{
		String requestedIdP = httpRequest.getParameter(PreferredAuthenticationHelper.IDP_SELECT_PARAM);
		OAuthClientProperties clientProperties = credentialExchange.getSettings();
		Set<String> keys = clientProperties.getStructuredListKeys(OAuthClientProperties.PROVIDERS);
		
		if (requestedIdP == null)
		{
			if (keys.size() > 1)
				throw new IllegalStateException("OAuth authentication option was not requested with " 
					+ PreferredAuthenticationHelper.IDP_SELECT_PARAM
					+ " and there are multiple options installed: "
					+ "can not perform automatic authentication.");
			return keys.iterator().next();
		}
		
		String authnOption = OAuthClientProperties.PROVIDERS + 
				AuthenticationOptionKeyUtils.decodeOption(requestedIdP) + ".";
		if (!keys.contains(authnOption))
			throw new IllegalStateException("Client requested authN option " + authnOption 
					+", which is not available in "
					+ "the authenticator selected for automated proxy authN. "
					+ "Ignoring the request.");
		return authnOption;
	}

	boolean triggerAutomatedAuthentication(HttpServletRequest httpRequest,
			HttpServletResponse httpResponse, String endpointPath) throws IOException
	{
		String idpKey = getIdpConfigKey(httpRequest);
		return startLogin(idpKey, httpRequest, httpResponse, endpointPath);
	}
	
	private boolean startLogin(String idpConfigKey, HttpServletRequest httpRequest,
			HttpServletResponse httpResponse, String endpointPath) throws IOException
	{
		HttpSession session = httpRequest.getSession();
		OAuthContext context = (OAuthContext) session.getAttribute(
				OAuth2Retrieval.REMOTE_AUTHN_CONTEXT);
		if (context != null)
		{
			log.debug("Ignoring automated login as the previous authentication "
					+ "is still in progress.");
			return false;
		}
		String currentRelativeURI = ProxyAuthenticationFilter.getCurrentRelativeURL(httpRequest);
		try
		{
			context = credentialExchange.createRequest(idpConfigKey, Optional.empty());
			context.setReturnUrl(currentRelativeURI);
			session.setAttribute(OAuth2Retrieval.REMOTE_AUTHN_CONTEXT, context);
			session.setAttribute(ProxyAuthenticationFilter.AUTOMATED_LOGIN_FIRED, "true");
		} catch (Exception e)
		{
			throw new IllegalStateException("Can not create OAuth2 authN request", e);
		}
		setLastIdpCookie(httpResponse, idpConfigKey, endpointPath);
		handleRequestInternal(context, httpRequest, httpResponse);
		return true;
	}
	
	private void handleRequestInternal(OAuthContext context,
			HttpServletRequest request, HttpServletResponse response) throws IOException
	{
		setCommonHeaders(response);
		String redirectURL = context.getRequestURI().toString();
		if (log.isDebugEnabled())
			log.debug("Starting OAuth redirection to OAuth provider: {}, returnURL is {}", 
					redirectURL, context.getReturnUrl());
		response.sendRedirect(redirectURL);
	}
	
	private void setCommonHeaders(HttpServletResponse response)
	{
		response.setHeader("Cache-Control","no-cache,no-store");
		response.setHeader("Pragma","no-cache");
	}
	
	private void setLastIdpCookie(HttpServletResponse httpResponse, String idpConfigKey, String endpointPath)
	{
		String optionId = idpConfigKey.substring(OAuthClientProperties.PROVIDERS.length(), idpConfigKey.length()-1);
		String selectedAuthn = AuthenticationOptionKeyUtils.encode(authenticatorId, optionId);
		Cookie lastIdpCookie = PreferredAuthenticationHelper.createLastIdpCookie(
				endpointPath, selectedAuthn);
		httpResponse.addCookie(lastIdpCookie);
	}
}
