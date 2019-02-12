/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as.token;

import java.util.Date;

import javax.ws.rs.core.Response;

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

import pl.edu.icm.unity.base.token.Token;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.authn.InvocationContext;
import pl.edu.icm.unity.engine.api.token.TokensManagement;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.oauth.as.OAuthASProperties;
import pl.edu.icm.unity.oauth.as.OAuthProcessor;
import pl.edu.icm.unity.oauth.as.OAuthToken;
import pl.edu.icm.unity.oauth.as.token.AccessTokenResource.OAuthErrorException;
import pl.edu.icm.unity.store.api.tx.TransactionalRunner;
import pl.edu.icm.unity.types.basic.EntityParam;

/**
 * Handles the fundamental OAuth authz code flow, from the perspective of the token endpoint.
 *  
 * @author K. Benedyczak
 */
class AuthzCodeHandler
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_OAUTH, AuthzCodeHandler.class);
	private TokensManagement tokensManagement;
	private OAuthASProperties config;
	private TransactionalRunner tx;
	
	AuthzCodeHandler(TokensManagement tokensManagement, OAuthASProperties config, TransactionalRunner tx)
	{
		this.tokensManagement = tokensManagement;
		this.config = config;
		this.tx = tx;
	}


	Response handleAuthzCodeFlow(String code, String redirectUri, String codeVerifier)
			throws EngineException, JsonProcessingException
	{
		TokensPair tokensPair;
		try
		{
			tokensPair = loadAndRemoveAuthzCodeToken(code);
		} catch (OAuthErrorException e)
		{
			return e.response;
		}

		Token codeToken = tokensPair.codeToken;
		OAuthToken parsedAuthzCodeToken = tokensPair.parsedAuthzCodeToken;

		try
		{
			verifyPKCE(parsedAuthzCodeToken, codeVerifier);
		} catch (OAuthErrorException e)
		{
			return e.response;
		} 
		
		if (parsedAuthzCodeToken.getRedirectUri() != null)
		{
			if (redirectUri == null)
				return BaseOAuthResource.makeError(OAuth2Error.INVALID_GRANT,
						"redirect_uri is required");
			if (!redirectUri.equals(parsedAuthzCodeToken.getRedirectUri()))
				return BaseOAuthResource.makeError(OAuth2Error.INVALID_GRANT,
						"redirect_uri is wrong");
		}

		OAuthToken internalToken = new OAuthToken(parsedAuthzCodeToken);
		AccessToken accessToken = OAuthProcessor.createAccessToken(internalToken);
		internalToken.setAccessToken(accessToken.getValue());

		Date now = new Date();
		RefreshToken refreshToken = TokenUtils.addRefreshToken(config, tokensManagement, 
				now, internalToken, codeToken.getOwner());
		Date accessExpiration = TokenUtils.getAccessTokenExpiration(config, now);

		AccessTokenResponse oauthResponse = TokenUtils.getAccessTokenResponse(internalToken,
				accessToken, refreshToken, null);
		log.debug("Authz code grant: issuing new access token {}, valid until {}", 
				BaseOAuthResource.tokenToLog(accessToken.getValue()), 
				accessExpiration);
		tokensManagement.addToken(OAuthProcessor.INTERNAL_ACCESS_TOKEN,
				accessToken.getValue(), new EntityParam(codeToken.getOwner()),
				internalToken.getSerialized(), now, accessExpiration);

		return BaseOAuthResource.toResponse(Response.ok(BaseOAuthResource.getResponseContent(oauthResponse)));
	}

	
	private void verifyPKCE(OAuthToken parsedAuthzCodeToken, String codeVerifier) throws OAuthErrorException
	{
		if (parsedAuthzCodeToken.getCodeChallenge() == null && 
				parsedAuthzCodeToken.getClientType() == ClientType.PUBLIC)
			throw new OAuthErrorException(
					BaseOAuthResource.makeError(OAuth2Error.INVALID_GRANT, "missing mandatory PKCE"));
		if (parsedAuthzCodeToken.getCodeChallenge() != null && codeVerifier == null)
			throw new OAuthErrorException(
					BaseOAuthResource.makeError(OAuth2Error.INVALID_GRANT, "missing PKCE"));
		if (parsedAuthzCodeToken.getCodeChallenge() == null && codeVerifier != null)
			throw new OAuthErrorException(
					BaseOAuthResource.makeError(OAuth2Error.INVALID_GRANT, "unexpected PKCE"));

		if (parsedAuthzCodeToken.getCodeChallenge() == null)
			return;

		String method = parsedAuthzCodeToken.getCodeChallengeMethod();
		if (method == null)
			method = CodeChallengeMethod.PLAIN.getValue();
		
		verifyPKCEChallenge(parsedAuthzCodeToken.getCodeChallenge(), codeVerifier, method);
	}


	private void verifyPKCEChallenge(String codeChallenge, String codeVerifier, String method) throws OAuthErrorException
	{
		CodeChallenge computedCodeChallenge = CodeChallenge.compute(CodeChallengeMethod.parse(method), 
				new CodeVerifier(codeVerifier));
		if (!computedCodeChallenge.getValue().equals(codeChallenge))
			throw new OAuthErrorException(
					BaseOAuthResource.makeError(OAuth2Error.INVALID_GRANT, "PKCE verification error"));
	}

	private TokensPair loadAndRemoveAuthzCodeToken(String code)
			throws OAuthErrorException, EngineException
	{
		return tx.runInTransactionRetThrowing(() -> {
			try
			{
				Token codeToken = tokensManagement.getTokenById(
						OAuthProcessor.INTERNAL_CODE_TOKEN, code);
				OAuthToken parsedAuthzCodeToken = BaseOAuthResource.parseInternalToken(codeToken);

				long callerEntityId = InvocationContext.getCurrent()
						.getLoginSession().getEntityId();
				if (parsedAuthzCodeToken.getClientId() != callerEntityId)
				{
					log.warn("Client with id " + callerEntityId
							+ " presented authorization code issued "
							+ "for client "
							+ parsedAuthzCodeToken.getClientId());
					// intended - we mask the reason
					throw new OAuthErrorException(BaseOAuthResource.makeError(
							OAuth2Error.INVALID_GRANT, "wrong code"));
				}
				tokensManagement.removeToken(OAuthProcessor.INTERNAL_CODE_TOKEN,
						code);
				return new TokensPair(codeToken, parsedAuthzCodeToken);
			} catch (IllegalArgumentException e)
			{
				throw new OAuthErrorException(
						BaseOAuthResource.makeError(OAuth2Error.INVALID_GRANT, "wrong code"));
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
