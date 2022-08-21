/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as.token;

import javax.ws.rs.FormParam;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import org.apache.logging.log4j.Logger;
import org.springframework.context.ApplicationEventPublisher;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nimbusds.oauth2.sdk.GrantType;
import com.nimbusds.oauth2.sdk.OAuth2Error;

import io.imunity.idp.LastIdPClinetAccessAttributeManagement;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.EndpointManagement;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.authn.InvocationContext;
import pl.edu.icm.unity.engine.api.idp.IdPEngine;
import pl.edu.icm.unity.engine.api.token.TokensManagement;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.oauth.as.OAuthASProperties;
import pl.edu.icm.unity.oauth.as.OAuthRequestValidator;
import pl.edu.icm.unity.oauth.as.webauthz.OAuthIdPEngine;
import pl.edu.icm.unity.store.api.tx.TransactionalRunner;
import pl.edu.icm.unity.types.endpoint.ResolvedEndpoint;

/**
 * RESTful implementation of the access token resource.
 * <p>
 * Access to this resource should be limited only to authenticated OAuth clients
 * 
 * @author K. Benedyczak
 * @author P. Piernik
 */
@Produces("application/json")
@Path(OAuthTokenEndpoint.TOKEN_PATH)
public class AccessTokenResource extends BaseOAuthResource
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_OAUTH, AccessTokenResource.class);

	public static final String ACCESS_TOKEN_TYPE_ID = "urn:ietf:params:oauth:token-type:access_token";
	public static final String ID_TOKEN_TYPE_ID = "urn:ietf:params:oauth:token-type:id_token";
	public static final String EXCHANGE_SCOPE = "token-exchange";

	private final AuthzCodeHandler authzCodeHandler;
	private final RefreshTokenHandler refreshTokenHandler;
	private final ExchangeTokenHandler exchangeTokenHandler;
	private final CredentialFlowHandler credentialFlowHandler;
	private final OAuthTokenStatisticPublisher statisticPublisher;

	public AccessTokenResource(TokensManagement tokensManagement, OAuthAccessTokenRepository accessTokensDAO,
			OAuthRefreshTokenRepository refreshTokensDAO, ClientTokensCleaner tokenCleaner, OAuthASProperties config,
			OAuthRequestValidator requestValidator, IdPEngine idpEngineInsecure, EntityManagement idMan,
			TransactionalRunner tx, ApplicationEventPublisher eventPublisher, MessageSource msg,
			EndpointManagement endpointMan,
			LastIdPClinetAccessAttributeManagement lastIdPClinetAccessAttributeManagement, ResolvedEndpoint endpoint)
	{
		OAuthIdPEngine notAuthorizedOauthIdpEngine = new OAuthIdPEngine(idpEngineInsecure);
		AccessTokenFactory accessTokenFactory = new AccessTokenFactory(config);
		this.statisticPublisher = new OAuthTokenStatisticPublisher(eventPublisher, msg, idMan, requestValidator,
				endpoint, endpointMan, lastIdPClinetAccessAttributeManagement);
		TokenUtils tokenUtils = new TokenUtils(requestValidator, config, notAuthorizedOauthIdpEngine);
		this.authzCodeHandler = new AuthzCodeHandler(tokensManagement, accessTokensDAO, refreshTokensDAO, config, tx,
				accessTokenFactory, statisticPublisher, tokenUtils);
		this.refreshTokenHandler = new RefreshTokenHandler(config, refreshTokensDAO, accessTokenFactory,
				accessTokensDAO, tokenCleaner, tokenUtils);
		this.exchangeTokenHandler = new ExchangeTokenHandler(config, refreshTokensDAO, accessTokenFactory,
				accessTokensDAO, tokenUtils, statisticPublisher, requestValidator, idMan);
		this.credentialFlowHandler = new CredentialFlowHandler(config,
				new ClientCredentialsProcessor(requestValidator, idpEngineInsecure, config), statisticPublisher,
				accessTokenFactory, accessTokensDAO, tokenUtils);
	}

	@Path("/")
	@POST
	public Response getToken(@FormParam("grant_type") String grantType, @FormParam("code") String code,
			@FormParam("scope") String scope, @FormParam("redirect_uri") String redirectUri,
			@FormParam("refresh_token") String refreshToken, @FormParam("audience") String audience,
			@FormParam("requested_token_type") String requestedTokenType,
			@FormParam("subject_token") String subjectToken, @FormParam("subject_token_type") String subjectTokenType,
			@FormParam("code_verifier") String codeVerifier, @HeaderParam("Accept") String acceptHeader)
			throws EngineException, JsonProcessingException
	{
		if (grantType == null)
		{
			statisticPublisher.reportFailAsLoggedClient();
			return makeError(OAuth2Error.INVALID_REQUEST, "grant_type is required");
		}

		if (!validateClientAuthenticationForNonCodeGrant(grantType))
			return makeError(OAuth2Error.INVALID_CLIENT, "not authenticated");

		log.trace("Handle new token request with " + grantType + " grant");

		if (grantType.equals(GrantType.AUTHORIZATION_CODE.getValue()))
		{
			if (code == null)
			{
				statisticPublisher.reportFailAsLoggedClient();
				return makeError(OAuth2Error.INVALID_REQUEST, "code is required");
			}
			return authzCodeHandler.handleAuthzCodeFlow(code, redirectUri, codeVerifier, acceptHeader);
		} else if (grantType.equals(GrantType.CLIENT_CREDENTIALS.getValue()))
		{
			return credentialFlowHandler.handleClientCredentialFlow(scope, acceptHeader);
		} else if (grantType.equals(GrantType.TOKEN_EXCHANGE.getValue()))
		{
			if (audience == null)
				return makeError(OAuth2Error.INVALID_REQUEST, "audience is required");
			if (subjectToken == null)
				return makeError(OAuth2Error.INVALID_REQUEST, "subject_token is required");
			if (subjectTokenType == null)
				return makeError(OAuth2Error.INVALID_REQUEST, "subject_token_type is required");
			return exchangeTokenHandler.handleExchangeToken(subjectToken, subjectTokenType, requestedTokenType,
					audience, scope, acceptHeader);
		} else if (grantType.equals(GrantType.REFRESH_TOKEN.getValue()))
		{
			if (refreshToken == null)
				return makeError(OAuth2Error.INVALID_REQUEST, "refresh_token is required");
			return refreshTokenHandler.handleRefreshToken(refreshToken, scope, acceptHeader);
		} else
		{
			return makeError(OAuth2Error.INVALID_GRANT, "wrong or not supported grant_type value");
		}
	}

	/**
	 * Authentication is optional for this REST path. However, this is only for the
	 * code grant (where we allow unauthenticated public clients secured by PKCE).
	 * So let's ensure for other cases that client's authn was performed.
	 */
	private boolean validateClientAuthenticationForNonCodeGrant(String grantType)
	{
		if (grantType.equals(GrantType.AUTHORIZATION_CODE.getValue()))
			return true;
		return InvocationContext.getCurrent().getLoginSession() != null;
	}

}
