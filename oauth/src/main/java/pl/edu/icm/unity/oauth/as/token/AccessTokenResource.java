/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as.token;

import java.util.Date;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import org.apache.log4j.Logger;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.oauth.as.OAuthASProperties;
import pl.edu.icm.unity.oauth.as.OAuthProcessor;
import pl.edu.icm.unity.oauth.as.OAuthToken;
import pl.edu.icm.unity.server.api.internal.Token;
import pl.edu.icm.unity.server.api.internal.TokensManagement;
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
import com.nimbusds.openid.connect.sdk.OIDCAccessTokenResponse;

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
	
	public AccessTokenResource(TokensManagement tokensManagement, OAuthASProperties config)
	{
		this.tokensManagement = tokensManagement;
		this.config = config;
	}

	@Path("/")
	@POST
	public Response getToken(@QueryParam("grant_type") String grantType, 
			@QueryParam("code") String code,
			@QueryParam("redirect_uri") String redirectUri) throws EngineException, JsonProcessingException
	{
		if (code == null)
			return makeError(OAuth2Error.INVALID_REQUEST, "code is required");
		if (grantType == null)
			return makeError(OAuth2Error.INVALID_REQUEST, "grant_type is required");
		if (!grantType.equals(GrantType.AUTHORIZATION_CODE.getValue()))
			return makeError(OAuth2Error.INVALID_GRANT, "wrong grant_type value");

		Token codeToken;
		OAuthToken parsedAuthzCodeToken;
		Object transaction = tokensManagement.startTokenTransaction();
		try
		{
			codeToken = tokensManagement.getTokenById(OAuthProcessor.INTERNAL_CODE_TOKEN, code, transaction);
			parsedAuthzCodeToken = parseInternalToken(codeToken);
			
			long callerEntityId = InvocationContext.getCurrent().getLoginSession().getEntityId();
			if (parsedAuthzCodeToken.getClientId() != callerEntityId)
			{
				log.warn("Client with id " + callerEntityId + " presented authorization code issued "
						+ "for client " + parsedAuthzCodeToken.getClientId());
				return makeError(OAuth2Error.INVALID_GRANT, "wrong code"); //intended - we mask the reason
			}
			tokensManagement.removeToken(OAuthProcessor.INTERNAL_CODE_TOKEN, code, transaction);
			tokensManagement.commitTokenTransaction(transaction);
		} catch (WrongArgumentException e)
		{
			return makeError(OAuth2Error.INVALID_GRANT, "wrong code");
		} finally
		{
			tokensManagement.closeTokenTransaction(transaction);
		}
		
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
		
		int accessTokenValidity = config.getIntValue(OAuthASProperties.ACCESS_TOKEN_VALIDITY);
		Date expiration = new Date(now.getTime() + accessTokenValidity * 1000);
		
		JWT signedJWT = decodeIDToken(internalToken);
		AccessTokenResponse oauthResponse = signedJWT == null ? 
				new AccessTokenResponse(accessToken, null) : 
				new OIDCAccessTokenResponse(accessToken, null, signedJWT);
		tokensManagement.addToken(OAuthProcessor.INTERNAL_ACCESS_TOKEN, accessToken.getValue(), 
				new EntityParam(codeToken.getOwner()), internalToken.getSerialized(), now, expiration);
		
		return toResponse(Response.ok(getResponseContent(oauthResponse)));
	}
}
