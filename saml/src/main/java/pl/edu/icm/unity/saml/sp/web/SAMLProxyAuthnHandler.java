/*
 * Copyright (c) 2017 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.sp.web;

import io.imunity.vaadin.auth.PreferredAuthenticationHelper;
import io.imunity.vaadin.auth.SigInInProgressContextService;
import io.imunity.vaadin.auth.server.ProxyAuthenticationFilter;
import io.imunity.vaadin.endpoint.common.LoginMachineDetailsExtractor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.apache.logging.log4j.Logger;
import pl.edu.icm.unity.base.authn.AuthenticationOptionKey;
import pl.edu.icm.unity.base.authn.AuthenticationOptionKeyUtils;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.authn.AuthenticationStepContext;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorStepContext;
import pl.edu.icm.unity.engine.api.authn.RememberMeToken.LoginMachineDetails;
import pl.edu.icm.unity.engine.api.authn.remote.AuthenticationTriggeringContext;
import pl.edu.icm.unity.saml.sp.RemoteAuthnContext;
import pl.edu.icm.unity.saml.sp.SAMLExchange;
import pl.edu.icm.unity.saml.sp.SamlContextManagement;
import pl.edu.icm.unity.saml.sp.config.TrustedIdPKey;

import java.io.IOException;
import java.util.Set;

/**
 * Support for automatic proxy authentication. Automatically redirects to external IdP.
 * 
 * @author K. Benedyczak
 */
public class SAMLProxyAuthnHandler
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_SAML,
			SAMLProxyAuthnHandler.class);
	
	private final SAMLExchange credentialExchange;
	private final SamlContextManagement samlContextManagement;
	private final String authenticatorId;

	public SAMLProxyAuthnHandler(SAMLExchange credentialExchange, SamlContextManagement samlContextManagement,
			String authenticatorId)
	{
		this.credentialExchange = credentialExchange;
		this.samlContextManagement = samlContextManagement;
		this.authenticatorId = authenticatorId;
	}
	
	public boolean triggerAutomatedAuthentication(HttpServletRequest httpRequest,
			HttpServletResponse httpResponse, String endpointPath, AuthenticatorStepContext context) throws IOException
	{
		TrustedIdPKey idpKey = getIdpConfigKey(httpRequest);
		return startLogin(idpKey, httpRequest, httpResponse, context);
	}

	private TrustedIdPKey getIdpConfigKey(HttpServletRequest httpRequest)
	{
		String requestedIdP = httpRequest.getParameter(PreferredAuthenticationHelper.IDP_SELECT_PARAM);
		Set<TrustedIdPKey> keys = credentialExchange.getTrustedIdpKeysWithWebBindings();
		
		if (requestedIdP == null)
		{
			if (keys.size() > 1)
				throw new IllegalStateException("SAML authentication option was not requested with " 
					+ PreferredAuthenticationHelper.IDP_SELECT_PARAM
					+ " and there are multiple options installed: "
					+ "can not perform automatic authentication.");
			return keys.iterator().next();
		}
		
		TrustedIdPKey authnOption = new TrustedIdPKey(AuthenticationOptionKeyUtils.decodeOption(requestedIdP));
		if (!keys.contains(authnOption))
			throw new IllegalStateException("Client requested authN option " + authnOption 
					+", which is not available in "
					+ "the authenticator selected for automated proxy authN. "
					+ "Ignoring the request.");
		return authnOption;
	}
	
	private boolean startLogin(TrustedIdPKey idpConfigKey, HttpServletRequest httpRequest,
			HttpServletResponse httpResponse, AuthenticatorStepContext authnContext) throws IOException
	{
		HttpSession session = httpRequest.getSession();
		
		String currentRelativeURI = ProxyAuthenticationFilter.getCurrentRelativeURL(httpRequest);
		log.info("Starting automatic proxy authentication with remote SAML IdP "
				+ "configured under {}, current relative URI is {}", idpConfigKey, currentRelativeURI);	
		LoginMachineDetails loginMachineDetails = LoginMachineDetailsExtractor.getLoginMachineDetailsFromCurrentRequest();
		RemoteAuthnContext context;
		try
		{
			AuthenticationStepContext authnStepContext = new AuthenticationStepContext(authnContext, 
					getAuthnOptionId(idpConfigKey), SigInInProgressContextService.getContext(httpRequest));
			context = credentialExchange.createSAMLRequest(idpConfigKey, currentRelativeURI, authnStepContext, 
					loginMachineDetails,
					currentRelativeURI,
					AuthenticationTriggeringContext.authenticationTriggeredFirstFactor());
			session.setAttribute(ProxyAuthenticationFilter.AUTOMATED_LOGIN_FIRED, "true");
			samlContextManagement.addAuthnContext(context);
		} catch (Exception e)
		{
			throw new IllegalStateException("Can not create SAML authN request", e);
		}
		
		RedirectRequestHandler.handleRequest(context, httpResponse);
		return true;
	}
	
	private AuthenticationOptionKey getAuthnOptionId(TrustedIdPKey idpConfigKey)
	{
		return new AuthenticationOptionKey(authenticatorId, idpConfigKey.asString());
	}
}
