/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as.token;

import java.util.Date;

import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import org.apache.log4j.Logger;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.oauth.as.OAuthASProperties;
import pl.edu.icm.unity.oauth.as.OAuthProcessor;
import pl.edu.icm.unity.oauth.as.OAuthRequestValidator;
import pl.edu.icm.unity.oauth.as.OAuthToken;
import pl.edu.icm.unity.oauth.as.OAuthValidationException;
import pl.edu.icm.unity.server.api.internal.IdPEngine;
import pl.edu.icm.unity.server.api.internal.Token;
import pl.edu.icm.unity.server.api.internal.TokensManagement;
import pl.edu.icm.unity.server.api.internal.TransactionalRunner;
import pl.edu.icm.unity.server.authn.InvocationContext;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.types.basic.EntityParam;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nimbusds.jwt.JWT;
import com.nimbusds.oauth2.sdk.AccessTokenResponse;
import com.nimbusds.oauth2.sdk.GrantType;
import com.nimbusds.oauth2.sdk.OAuth2Error;
import com.nimbusds.oauth2.sdk.token.AccessToken;
import com.nimbusds.oauth2.sdk.token.BearerAccessToken;
import com.nimbusds.oauth2.sdk.token.Tokens;
import com.nimbusds.openid.connect.sdk.OIDCTokenResponse;
import com.nimbusds.openid.connect.sdk.token.OIDCTokens;

/**
 * RESTful implementation of the access token resource.
 * <p>
 * Access to this resource should be limited only to authenticated OAuth clients
 * 
 * @author K. Benedyczak
 */
@Produces("application/json")
@Path(OAuthTokenEndpoint.TOKEN_PATH)
public class AccessTokenResource extends BaseOAuthResource
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_OAUTH, AccessTokenResource.class);
	
	private TokensManagement tokensManagement;
	private OAuthASProperties config;
	private TransactionalRunner tx;
	private ClientCredentialsProcessor clientGrantProcessor;
	
	public AccessTokenResource(TokensManagement tokensManagement, OAuthASProperties config, 
			OAuthRequestValidator requestValidator, IdPEngine idpEngine,
			TransactionalRunner tx)
	{
		this.tokensManagement = tokensManagement;
		this.config = config;
		this.tx = tx;
		this.clientGrantProcessor = new ClientCredentialsProcessor(requestValidator, idpEngine, config);
	}

	@Path("/")
	@POST
	public Response getToken(@FormParam("grant_type") String grantType, 
			@FormParam("code") String code,
			@FormParam("scope") String scope,
			@FormParam("redirect_uri") String redirectUri) throws EngineException, JsonProcessingException
	{
		if (grantType == null)
			return makeError(OAuth2Error.INVALID_REQUEST, "grant_type is required");
		
		if (grantType.equals(GrantType.AUTHORIZATION_CODE.getValue()))
		{
			if (code == null)
				return makeError(OAuth2Error.INVALID_REQUEST, "code is required");
			return handleAuthzCodeFlow(code, redirectUri);
		} else if (grantType.equals(GrantType.CLIENT_CREDENTIALS.getValue()))
		{
			return handleClientCredentialFlow(scope);
		} else
		{
			return makeError(OAuth2Error.INVALID_GRANT, "wrong or not supported grant_type value");
		}
	}

	private Response handleClientCredentialFlow(String scope) 
			throws EngineException, JsonProcessingException
	{
		Date now = new Date();
		AccessToken accessToken = new BearerAccessToken();
		OAuthToken internalToken;
		try
		{
			internalToken = clientGrantProcessor.processClientFlowRequest(accessToken.getValue(), scope);
		} catch (OAuthValidationException e)
		{
			return makeError(OAuth2Error.INVALID_REQUEST, e.getMessage());
		}
		
		Date expiration = getAccessTokenExpiration(now);
		
		AccessTokenResponse oauthResponse = new AccessTokenResponse(new Tokens(accessToken, null));
		tokensManagement.addToken(OAuthProcessor.INTERNAL_ACCESS_TOKEN, accessToken.getValue(), 
				new EntityParam(internalToken.getClientId()), internalToken.getSerialized(), now, expiration);
		
		return toResponse(Response.ok(getResponseContent(oauthResponse)));
	}
	

	private Response handleAuthzCodeFlow(String code, String redirectUri) 
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
		
		if (parsedAuthzCodeToken.getRedirectUri() != null)
		{
			if (redirectUri == null)
				return makeError(OAuth2Error.INVALID_GRANT, "redirect_uri is required");
			if (!redirectUri.equals(parsedAuthzCodeToken.getRedirectUri()))
				return makeError(OAuth2Error.INVALID_GRANT, "redirect_uri is wrong");
		}
		
		Date now = new Date();
		AccessToken accessToken = new BearerAccessToken();
		OAuthToken internalToken = new OAuthToken(parsedAuthzCodeToken);
		internalToken.setAccessToken(accessToken.getValue());
		
		Date expiration = getAccessTokenExpiration(now);
		
		JWT signedJWT = decodeIDToken(internalToken);
		AccessTokenResponse oauthResponse = signedJWT == null ? 
				new AccessTokenResponse(new Tokens(accessToken, null)) : 
				new OIDCTokenResponse(new OIDCTokens(signedJWT, accessToken, null));
		tokensManagement.addToken(OAuthProcessor.INTERNAL_ACCESS_TOKEN, accessToken.getValue(), 
				new EntityParam(codeToken.getOwner()), internalToken.getSerialized(), now, expiration);
		
		return toResponse(Response.ok(getResponseContent(oauthResponse)));
	}
	
	
	private Date getAccessTokenExpiration(Date now)
	{
		int accessTokenValidity = config.getIntValue(OAuthASProperties.ACCESS_TOKEN_VALIDITY);
		return new Date(now.getTime() + accessTokenValidity * 1000);
	}
	
	private TokensPair loadAndRemoveAuthzCodeToken(String code) throws OAuthErrorException, EngineException
	{
		return tx.runInTransacitonRet(() -> {
			try
			{
				Token codeToken = tokensManagement.getTokenById(
						OAuthProcessor.INTERNAL_CODE_TOKEN, code);
				OAuthToken parsedAuthzCodeToken = parseInternalToken(codeToken);
				
				long callerEntityId = InvocationContext.getCurrent().getLoginSession().getEntityId();
				if (parsedAuthzCodeToken.getClientId() != callerEntityId)
				{
					log.warn("Client with id " + callerEntityId + " presented authorization code issued "
							+ "for client " + parsedAuthzCodeToken.getClientId());
					 //intended - we mask the reason
					throw new OAuthErrorException(makeError(OAuth2Error.INVALID_GRANT, "wrong code"));
				}
				tokensManagement.removeToken(OAuthProcessor.INTERNAL_CODE_TOKEN, code);
				return new TokensPair(codeToken, parsedAuthzCodeToken);
			} catch (WrongArgumentException e)
			{
				throw new OAuthErrorException(makeError(OAuth2Error.INVALID_GRANT, "wrong code"));
			}
		});
	}
	
	private static class OAuthErrorException extends EngineException
	{
		private Response response;

		public OAuthErrorException(Response response)
		{
			this.response = response;
		}
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
