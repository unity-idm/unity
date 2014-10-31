/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as.token;

import java.net.URI;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.Date;

import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.apache.log4j.Logger;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.InternalException;
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
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.oauth2.sdk.ErrorObject;
import com.nimbusds.oauth2.sdk.GrantType;
import com.nimbusds.oauth2.sdk.OAuth2Error;
import com.nimbusds.oauth2.sdk.SerializeException;
import com.nimbusds.oauth2.sdk.TokenErrorResponse;
import com.nimbusds.oauth2.sdk.http.HTTPResponse;
import com.nimbusds.oauth2.sdk.token.AccessToken;
import com.nimbusds.oauth2.sdk.token.BearerAccessToken;
import com.nimbusds.oauth2.sdk.token.BearerTokenError;
import com.nimbusds.openid.connect.sdk.AuthenticationSuccessResponse;

/**
 * RESTful implementation of the user information token resource
 * 
 * @author K. Benedyczak
 */
@Produces("application/json")
@Path("/userinfo")
public class UserInfoResource extends BaseOAuthResource
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_OAUTH, UserInfoResource.class);
	
	private TokensManagement tokensManagement;
	private OAuthASProperties config;
	
	public UserInfoResource(TokensManagement tokensManagement, OAuthASProperties config)
	{
		this.tokensManagement = tokensManagement;
		this.config = config;
	}

	@Path("/")
	@POST
	public Response getToken(@HeaderParam("Authorization") String bearerToken) 
			throws EngineException, JsonProcessingException
	{
		if (bearerToken == null)
			return makeError(BearerTokenError.MISSING_TOKEN, "To access the user info endpoint "
					+ "an access token must be used for authorization");
		
		BearerAccessToken accessToken;
		try
		{
			accessToken = BearerAccessToken.parse(bearerToken);
		} catch (ParseException e)
		{
			return makeError(BearerTokenError.INVALID_TOKEN, "must use Bearer access token");
		}
		
		Token internalAccessToken;
		try
		{
			internalAccessToken = tokensManagement.getTokenById(OAuthProcessor.INTERNAL_ACCESS_TOKEN, 
					accessToken.getValue());
		} catch (WrongArgumentException e)
		{
			return makeError(BearerTokenError.INVALID_TOKEN, "wrong token");
		}
		
		OAuthToken parsedAccessToken = parseInternalToken(internalAccessToken);
		
		
		
		
		Date now = new Date();
		AccessToken accessToken = new BearerAccessToken();
		OAuthToken internalToken = new OAuthToken(parsedAuthzCodeToken);
		internalToken.setAccessToken(accessToken.getValue());
		
		int accessTokenValidity = config.getIntValue(OAuthASProperties.ACCESS_TOKEN_VALIDITY);
		Date expiration = new Date(now.getTime() + accessTokenValidity * 1000);
		
		JWT signedJWT = decodeIDToken(internalToken);
		AuthenticationSuccessResponse oauthResponse = new AuthenticationSuccessResponse(
					toURI(redirectUri), null, signedJWT, accessToken, null);
		tokensManagement.addToken(OAuthProcessor.INTERNAL_ACCESS_TOKEN, accessToken.getValue(), 
				new EntityParam(accessToken.getOwner()), internalToken.getSerialized(), now, expiration);
		
		return toResponse(Response.ok(getResponseContent(oauthResponse)));
	}
}
