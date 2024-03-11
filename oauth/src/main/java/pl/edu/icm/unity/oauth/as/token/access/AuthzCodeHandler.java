/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as.token.access;

import java.util.Date;

import jakarta.ws.rs.core.Response;

import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nimbusds.oauth2.sdk.AccessTokenResponse;
import com.nimbusds.oauth2.sdk.OAuth2Error;
import com.nimbusds.oauth2.sdk.client.ClientType;
import com.nimbusds.oauth2.sdk.pkce.CodeChallenge;
import com.nimbusds.oauth2.sdk.pkce.CodeChallengeMethod;
import com.nimbusds.oauth2.sdk.pkce.CodeVerifier;
import com.nimbusds.oauth2.sdk.token.AccessToken;
import com.nimbusds.oauth2.sdk.token.RefreshToken;
import com.nimbusds.openid.connect.sdk.OIDCTokenResponse;

import pl.edu.icm.unity.base.entity.EntityParam;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.token.Token;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.authn.InvocationContext;
import pl.edu.icm.unity.engine.api.authn.LoginSession;
import pl.edu.icm.unity.engine.api.token.TokensManagement;
import pl.edu.icm.unity.oauth.as.OAuthASProperties;
import pl.edu.icm.unity.oauth.as.OAuthProcessor;
import pl.edu.icm.unity.oauth.as.OAuthToken;
import pl.edu.icm.unity.oauth.as.OAuthToken.PKCSInfo;
import pl.edu.icm.unity.oauth.as.token.BaseOAuthResource;
import pl.edu.icm.unity.oauth.as.token.OAuthErrorException;
import pl.edu.icm.unity.store.api.tx.TransactionalRunner;

/**
 * Handles the fundamental OAuth authz code flow, from the perspective of the
 * token endpoint.
 * 
 * @author K. Benedyczak
 */
class AuthzCodeHandler
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_OAUTH, AuthzCodeHandler.class);
	private final TokensManagement tokensManagement;
	private final OAuthASProperties config;
	private final TransactionalRunner tx;
	private final AccessTokenFactory accessTokenFactory;
	private final OAuthAccessTokenRepository accessTokenDAO;
	private final OAuthRefreshTokenRepository refreshTokenRepository;
	private final OAuthTokenStatisticPublisher statisticsPublisher;
	private final TokenService tokenService;

	AuthzCodeHandler(TokensManagement tokensManagement, OAuthAccessTokenRepository accessTokenDAO,
			OAuthRefreshTokenRepository refreshTokenDAO, TransactionalRunner tx, AccessTokenFactory accesstokenFactory,
			OAuthTokenStatisticPublisher statisticsPublisher, OAuthASProperties config, TokenService tokenService)
	{
		this.tokensManagement = tokensManagement;
		this.accessTokenDAO = accessTokenDAO;
		this.refreshTokenRepository = refreshTokenDAO;
		this.config = config;
		this.tx = tx;
		this.accessTokenFactory = accesstokenFactory;
		this.statisticsPublisher = statisticsPublisher;
		this.tokenService = tokenService;
	}

	Response handleAuthzCodeFlow(String code, String redirectUri, String codeVerifier, String acceptHeader)
			throws EngineException, JsonProcessingException
	{
		TokensPair tokensPair;
		try
		{
			tokensPair = loadAndRemoveAuthzCodeToken(code);
		} catch (OAuthErrorException e)
		{
			statisticsPublisher.reportFailAsLoggedClient();
			return e.response;
		}

		Token codeToken = tokensPair.codeToken;
		OAuthToken parsedAuthzCodeToken = tokensPair.parsedAuthzCodeToken;

		try
		{
			assertConfidentialClientHasSession(parsedAuthzCodeToken.getClientType());
			assertPublicClientHasPKCE(parsedAuthzCodeToken.getPkcsInfo(), parsedAuthzCodeToken.getClientType());
			verifyPKCEIfDefined(parsedAuthzCodeToken.getPkcsInfo(), parsedAuthzCodeToken.getClientType(), codeVerifier);

		} catch (OAuthErrorException e)
		{
			statisticsPublisher.reportFail(parsedAuthzCodeToken.getClientUsername(),
					parsedAuthzCodeToken.getClientName());
			return e.response;
		}

		if (parsedAuthzCodeToken.getRedirectUri() != null)
		{
			if (redirectUri == null)
			{
				statisticsPublisher.reportFail(parsedAuthzCodeToken.getClientUsername(),
						parsedAuthzCodeToken.getClientName());
				return BaseOAuthResource.makeError(OAuth2Error.INVALID_GRANT, "redirect_uri is required");
			}
			if (!redirectUri.equals(parsedAuthzCodeToken.getRedirectUri()))
			{
				statisticsPublisher.reportFail(parsedAuthzCodeToken.getClientUsername(),
						parsedAuthzCodeToken.getClientName());
				return BaseOAuthResource.makeError(OAuth2Error.INVALID_GRANT, "redirect_uri is wrong");
			}
		}

		OAuthToken internalToken = new OAuthToken(parsedAuthzCodeToken);
		Date now = new Date();
		AccessToken accessToken = accessTokenFactory.create(internalToken, now, acceptHeader);
		internalToken.setAccessToken(accessToken.getValue());

		RefreshToken refreshToken = refreshTokenRepository
				.createRefreshToken(config, now, internalToken, codeToken.getOwner()).orElse(null);

		Date accessExpiration = TokenUtils.getAccessTokenExpiration(config, now);

		AccessTokenResponse oauthResponse = tokenService.getAccessTokenResponse(internalToken, accessToken, refreshToken,
				null);
		log.info("Authz code grant: issuing new access token {}, valid until {}",
				BaseOAuthResource.tokenToLog(accessToken.getValue()), accessExpiration);
		if (oauthResponse instanceof OIDCTokenResponse)
			log.debug("Issued OIDC ID token {}", ((OIDCTokenResponse)oauthResponse).getOIDCTokens().getIDTokenString());
		accessTokenDAO.storeAccessToken(accessToken, internalToken, new EntityParam(codeToken.getOwner()), now,
				accessExpiration);

		statisticsPublisher.reportSuccess(internalToken.getClientUsername(), internalToken.getClientName(),
				new EntityParam(codeToken.getOwner()));

		return BaseOAuthResource.toResponse(Response.ok(BaseOAuthResource.getResponseContent(oauthResponse)));
	}

	private void assertConfidentialClientHasSession(ClientType clientType) throws OAuthErrorException
	{
		if (clientType == ClientType.CONFIDENTIAL && InvocationContext.getCurrent()
				.getLoginSession() == null)
		{
			throw new OAuthErrorException(BaseOAuthResource.makeError(OAuth2Error.INVALID_CLIENT, "not authenticated"));
		}
	}
	
	private void assertPublicClientHasPKCE(PKCSInfo parsedAuthzCodeToken, ClientType clientType) throws OAuthErrorException
	{
		if (parsedAuthzCodeToken.getCodeChallenge() == null && clientType == ClientType.PUBLIC)
			throw new OAuthErrorException(
					BaseOAuthResource.makeError(OAuth2Error.INVALID_GRANT, "missing mandatory PKCE"));
	}

	private void verifyPKCEIfDefined(PKCSInfo parsedAuthzCodeToken, ClientType clientType, String codeVerifier)
			throws OAuthErrorException
	{
		if (parsedAuthzCodeToken.getCodeChallenge() != null && codeVerifier == null)
			throw new OAuthErrorException(BaseOAuthResource.makeError(OAuth2Error.INVALID_GRANT, "missing PKCE"));
		if (parsedAuthzCodeToken.getCodeChallenge() == null && codeVerifier != null)
			throw new OAuthErrorException(BaseOAuthResource.makeError(OAuth2Error.INVALID_GRANT, "unexpected PKCE"));

		if (parsedAuthzCodeToken.getCodeChallenge() == null)
			return;

		String method = parsedAuthzCodeToken.getCodeChallengeMethod();
		if (method == null)
			method = CodeChallengeMethod.PLAIN.getValue();

		verifyPKCEChallenge(parsedAuthzCodeToken.getCodeChallenge(), codeVerifier, method);
	}

	private void verifyPKCEChallenge(String codeChallenge, String codeVerifier, String method)
			throws OAuthErrorException
	{
		CodeChallenge computedCodeChallenge;
		try
		{
			computedCodeChallenge = CodeChallenge.compute(CodeChallengeMethod.parse(method),
					new CodeVerifier(codeVerifier));
		} catch (Exception e)
		{
			log.warn("Failure to parse code challenge or verifier", e);
			throw new OAuthErrorException(
					BaseOAuthResource.makeError(OAuth2Error.INVALID_GRANT, "PKCE verification error"));
		}
		if (!computedCodeChallenge.getValue().equals(codeChallenge))
			throw new OAuthErrorException(
					BaseOAuthResource.makeError(OAuth2Error.INVALID_GRANT, "PKCE verification error"));
	}

	private TokensPair loadAndRemoveAuthzCodeToken(String code) throws OAuthErrorException, EngineException
	{
		return tx.runInTransactionRetThrowing(() ->
		{
			try
			{
				Token codeToken = tokensManagement.getTokenById(OAuthProcessor.INTERNAL_CODE_TOKEN, code);
				OAuthToken parsedAuthzCodeToken = BaseOAuthResource.parseInternalToken(codeToken);

				LoginSession loginSession = InvocationContext.getCurrent().getLoginSession();
				if (loginSession != null && parsedAuthzCodeToken.getClientId() != loginSession.getEntityId())
				{
					log.warn("Client with id {} presented authorization code issued for client {}",
							loginSession.getEntityId(), parsedAuthzCodeToken.getClientId());
					// intended - we mask the reason
					throw new OAuthErrorException(BaseOAuthResource.makeError(OAuth2Error.INVALID_GRANT, "wrong code"));
				}
				tokensManagement.removeToken(OAuthProcessor.INTERNAL_CODE_TOKEN, code);
				return new TokensPair(codeToken, parsedAuthzCodeToken);
			} catch (IllegalArgumentException e)
			{
				throw new OAuthErrorException(BaseOAuthResource.makeError(OAuth2Error.INVALID_GRANT, "wrong code"));
			}
		});
	}

	private static class TokensPair
	{
		Token codeToken;
		OAuthToken parsedAuthzCodeToken;

		public TokensPair(Token codeToken, OAuthToken parsedAuthzCodeToken)
		{
			this.codeToken = codeToken;
			this.parsedAuthzCodeToken = parsedAuthzCodeToken;
		}
	}

}
