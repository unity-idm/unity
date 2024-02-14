/*
 * Copyright (c) 2024 Bixbit s.c. All rights reserved.
 *  See LICENSE file for licensing information.
 */
package io.imunity.jwt;

import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.authn.AbstractCredentialRetrieval;
import pl.edu.icm.unity.engine.api.authn.AbstractCredentialRetrievalFactory;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult.ResolvableError;
import pl.edu.icm.unity.engine.api.authn.AuthenticationStepContext;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorStepContext;
import pl.edu.icm.unity.engine.api.authn.InteractiveAuthenticationProcessor;
import pl.edu.icm.unity.engine.api.authn.InteractiveAuthenticationProcessor.PostAuthenticationStepDecision;
import pl.edu.icm.unity.engine.api.authn.RememberMeToken.LoginMachineDetails;
import pl.edu.icm.unity.engine.api.authn.RemoteAuthenticationResult;
import pl.edu.icm.unity.rest.jwt.authn.JWTExchange;
import pl.edu.icm.unity.types.authn.AuthenticationOptionKey;
import pl.edu.icm.unity.webui.authn.LoginMachineDetailsExtractor;
import pl.edu.icm.unity.webui.authn.PreferredAuthenticationHelper;
import pl.edu.icm.unity.webui.authn.ProxyAuthenticationCapable;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication;
import pl.edu.icm.unity.webui.authn.remote.BareSessionReinitializer;

@Component(JWTRetrieval.RETRIEVAL_NAME)
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
class JWTRetrieval extends AbstractCredentialRetrieval<JWTExchange>
		implements VaadinAuthentication, ProxyAuthenticationCapable
{
	private static final Logger LOG = Log.getLogger(Log.U_SERVER_WEB, JWTRetrieval.class);
	private static final String RETRIEVAL_NAME = "HttpPostFormJWTRetrievalFactory";
	private static final String POST_HTTP_METHOD = "POST";
	private static final String JWT_FORM_PARAM_NAME = "jwt";
	private static final String AUTHZ_LOGIN_TOKEN_FORM_PARAM_NAME = "authzLoginToken";
	
	public static final String NAME = "http-jwt";
	public static final String DESC = JWTRetrieval.RETRIEVAL_NAME + ".desc";
	
	private final InteractiveAuthenticationProcessor authnProcessor;
	private final AuthzLoginTokenService authzLoginTokenService;
	
	JWTRetrieval(InteractiveAuthenticationProcessor authnProcessor,
			AuthzLoginTokenService authzLoginTokenService)
	{
		super(VaadinAuthentication.NAME);
		this.authnProcessor = authnProcessor;
		this.authzLoginTokenService = authzLoginTokenService;
	}

	@Component
	public static class HttpPostFormJWTRetrievalFactory extends AbstractCredentialRetrievalFactory<JWTRetrieval>
	{
		@Autowired
		public HttpPostFormJWTRetrievalFactory(ObjectFactory<JWTRetrieval> factory)
		{
			super(NAME, DESC, VaadinAuthentication.NAME, factory, JWTExchange.ID);
		}
	}

	@Override 
	public boolean triggerAutomatedAuthentication(HttpServletRequest httpRequest,
			HttpServletResponse httpResponse,
			String endpointPath,
			AuthenticatorStepContext authnContext) throws IOException
	{
		if (POST_HTTP_METHOD.equals(httpRequest.getMethod()))
		{
			String token = getToken(httpRequest)
					.orElseThrow(InvalidJWTInPayloadException::new);
			AuthzLoginTokenContext loginContext = getAuthzAccessTokenContext(httpRequest)
					.orElseThrow(InvalidLoginTokenInPayloadException::new);
			
			assertTokenHashIsValid(token, loginContext.jwtHash);
			
			AuthenticationResult autnResult = checkJWT(token);

			assertEntityIsWhiteListed(autnResult);

			PostAuthenticationStepDecision postFirstFactorDecision = processFirstFactorResult(httpRequest,
					httpResponse, autnResult, authnContext);
			
			switch (postFirstFactorDecision.getDecision())
			{
				case COMPLETED:
					LOG.trace("Authentication completed");
					httpResponse.sendRedirect(loginContext.redirectURL.toString());
					return true;
				default:
					LOG.error("Retrieval does not support: {}", postFirstFactorDecision.getDecision());
					throw new InvalidRetrievalConfigurationException();
			}
		}
		return false;
	}

	private void assertEntityIsWhiteListed(AuthenticationResult autnResult)
	{
		LOG.trace("Asserting if matched entity is allowed to login");
	}

	private void assertTokenHashIsValid(String token, String jwtHash)
	{
		LOG.trace("Asserting if given token matches hash");
	}

	private Optional<AuthzLoginTokenContext> getAuthzAccessTokenContext(HttpServletRequest httpRequest)
	{
		String authzLoginToken = httpRequest.getParameter(AUTHZ_LOGIN_TOKEN_FORM_PARAM_NAME);
		if (isEmpty(authzLoginToken))
		{
			return Optional.empty();
		}
		try
		{
			return Optional.of(authzLoginTokenService.getAuthzLoginTokenContext(authzLoginToken));
		} catch (Exception ex)
		{
			LOG.error("Failed to match login token {} w/ context", ex);
		}
		return Optional.empty();
	}

	private PostAuthenticationStepDecision processFirstFactorResult(HttpServletRequest httpRequest,
			HttpServletResponse httpResponse,
			AuthenticationResult result,
			AuthenticatorStepContext authnContext)
	{
		LoginMachineDetails loginMachineDetails = LoginMachineDetailsExtractor.getLoginMachineDetailsFromCurrentRequest();
		AuthenticationOptionKey authnOptionId = getAuthnOptionId(getIdpConfigKey(httpRequest));
		AuthenticationStepContext authnStepContext = new AuthenticationStepContext(authnContext, authnOptionId);
		return authnProcessor.processFirstFactorResult(result, authnStepContext, loginMachineDetails, false,
				httpRequest, httpResponse, new BareSessionReinitializer(httpRequest));
	}

	private AuthenticationResult checkJWT(String token)
	{
		try
		{
			return credentialExchange.checkJWT(token);
		} catch (Exception e)
		{
			LOG.error("Runtime error during JWT response processing or principal mapping", e);
			return RemoteAuthenticationResult.failed(null, e, new ResolvableError("WebJWTRetrieval.authnFailedError"));
		}
	}
	
	private String getIdpConfigKey(HttpServletRequest httpRequest)
	{
		return httpRequest.getParameter(PreferredAuthenticationHelper.IDP_SELECT_PARAM);
	}
	
	private AuthenticationOptionKey getAuthnOptionId(String idpConfigKey)
	{
		return AuthenticationOptionKey.valueOf(idpConfigKey);
	}

	private Optional<String> getToken(HttpServletRequest httpRequest)
	{
		String jwt = httpRequest.getParameter(JWT_FORM_PARAM_NAME);
		if (jwt == null)
		{
			return Optional.empty();
		}
		int firstDot = jwt.indexOf('.');
		if (firstDot == -1)
		{
			return Optional.empty();
		}
		int secDot = jwt.indexOf('.', firstDot+1);
		if (secDot == -1)
		{
			return Optional.empty();
		}
		if (jwt.indexOf('.', secDot+1) != -1)
		{
			return Optional.empty();
		}
		return Optional.of(jwt);
	}

	@Override
	public void triggerAutomatedUIAuthentication(VaadinAuthenticationUI authenticatorUI)
	{
	}
	
	@Override
	public String getSerializedConfiguration()
	{
		return null;
	}

	@Override
	public void setSerializedConfiguration(String config)
	{
	}

	@Override
	public Collection<VaadinAuthenticationUI> createUIInstance(Context context,
			AuthenticatorStepContext authenticatorContext)
	{
		return List.of();
	}

	@Override
	public boolean supportsGrid()
	{
		return false;
	}

	@Override
	public boolean isMultiOption()
	{
		return false;
	}
	
	private static class InvalidJWTInPayloadException extends IllegalStateException {}
	private static class InvalidLoginTokenInPayloadException extends IllegalStateException {}
	private static class InvalidRetrievalConfigurationException extends IllegalStateException {}
}
