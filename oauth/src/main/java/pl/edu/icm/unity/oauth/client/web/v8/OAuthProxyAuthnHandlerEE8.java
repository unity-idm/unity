/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.client.web.v8;

import pl.edu.icm.unity.webui.authn.LoginMachineDetailsExtractor;
import org.apache.logging.log4j.Logger;
import pl.edu.icm.unity.base.authn.AuthenticationOptionKey;
import pl.edu.icm.unity.base.authn.AuthenticationOptionKeyUtils;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.authn.AuthenticationStepContext;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorStepContext;
import pl.edu.icm.unity.engine.api.authn.RememberMeToken.LoginMachineDetails;
import pl.edu.icm.unity.engine.api.authn.remote.AuthenticationTriggeringContext;
import pl.edu.icm.unity.oauth.client.OAuthContext;
import pl.edu.icm.unity.oauth.client.OAuthExchange;
import pl.edu.icm.unity.oauth.client.config.OAuthClientProperties;
import pl.edu.icm.unity.webui.authn.PreferredAuthenticationHelper;
import pl.edu.icm.unity.webui.authn.ProxyAuthenticationFilter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Optional;
import java.util.Set;

/**
 * Support for automatic proxy authentication. Automatically redirects to external AS.
 * 
 * @author K. Benedyczak
 */
public class OAuthProxyAuthnHandlerEE8
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_OAUTH,
			OAuthProxyAuthnHandlerEE8.class);

	private final OAuthExchange credentialExchange;
	private final String authenticatorId;

	public OAuthProxyAuthnHandlerEE8(OAuthExchange credentialExchange, String authenticatorId)
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

	public boolean triggerAutomatedAuthentication(HttpServletRequest httpRequest,
			HttpServletResponse httpResponse, String endpointPath, AuthenticatorStepContext context) throws IOException
	{
		String idpKey = getIdpConfigKey(httpRequest);
		return startLogin(idpKey, httpRequest, httpResponse, endpointPath, context);
	}
	
	private boolean startLogin(String idpConfigKey, HttpServletRequest httpRequest,
			HttpServletResponse httpResponse, String endpointPath, AuthenticatorStepContext authnContext) throws IOException
	{
		HttpSession session = httpRequest.getSession();
		String currentURI = endpointPath + ProxyAuthenticationFilter.getCurrentRelativeURL(httpRequest);
		LoginMachineDetails loginMachineDetails = LoginMachineDetailsExtractor.getLoginMachineDetailsFromCurrentRequest();
		OAuthContext context;
		try
		{
			context = credentialExchange.createRequest(idpConfigKey, Optional.empty(), 
					new AuthenticationStepContext(authnContext, getAuthnOptionId(idpConfigKey)),
					loginMachineDetails,
					currentURI,
					AuthenticationTriggeringContext.authenticationTriggeredFirstFactor());
			context.setReturnUrl(currentURI);
			session.setAttribute(ProxyAuthenticationFilter.AUTOMATED_LOGIN_FIRED, "true");
		} catch (Exception e)
		{
			throw new IllegalStateException("Can not create OAuth2 authN request", e);
		}
		handleRequestInternal(context, httpRequest, httpResponse);
		return true;
	}
	
	private void handleRequestInternal(OAuthContext context,
			HttpServletRequest request, HttpServletResponse response) throws IOException
	{
		setCommonHeaders(response);
		String redirectURL = context.getRequestURI().toString();
		log.info("Starting OAuth redirection to OAuth provider: {}, returnURL is {}", 
					redirectURL, context.getReturnUrl());
		response.sendRedirect(redirectURL);
	}
	
	private void setCommonHeaders(HttpServletResponse response)
	{
		response.setHeader("Cache-Control","no-cache,no-store");
		response.setHeader("Pragma","no-cache");
	}
	
	private AuthenticationOptionKey getAuthnOptionId(String idpConfigKey)
	{
		String optionId = idpConfigKey.substring(OAuthClientProperties.PROVIDERS.length(), idpConfigKey.length()-1);
		return new AuthenticationOptionKey(authenticatorId, optionId);
	}
}
