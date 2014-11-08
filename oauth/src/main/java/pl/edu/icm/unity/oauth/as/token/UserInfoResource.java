/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as.token;

import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import org.apache.http.HttpHeaders;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.oauth.as.OAuthProcessor;
import pl.edu.icm.unity.oauth.as.OAuthToken;
import pl.edu.icm.unity.server.api.internal.Token;
import pl.edu.icm.unity.server.api.internal.TokensManagement;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.token.BearerAccessToken;
import com.nimbusds.oauth2.sdk.token.BearerTokenError;

/**
 * RESTful implementation of the user information token resource
 * 
 * @author K. Benedyczak
 */
@Produces("application/json")
@Path(OAuthTokenEndpoint.USER_INFO_PATH)
public class UserInfoResource extends BaseOAuthResource
{
	private TokensManagement tokensManagement;
	
	public UserInfoResource(TokensManagement tokensManagement)
	{
		this.tokensManagement = tokensManagement;
	}

	@Path("/")
	@GET
	public Response getToken(@HeaderParam("Authorization") String bearerToken) 
			throws EngineException, JsonProcessingException
	{
		if (bearerToken == null)
			return makeBearerError(BearerTokenError.MISSING_TOKEN, "To access the user info endpoint "
					+ "an access token must be used for authorization");
		
		BearerAccessToken accessToken;
		try
		{
			accessToken = BearerAccessToken.parse(bearerToken);
		} catch (ParseException e)
		{
			return makeBearerError(BearerTokenError.INVALID_TOKEN, "Must use Bearer access token");
		}
		
		Token internalAccessToken;
		try
		{
			internalAccessToken = tokensManagement.getTokenById(OAuthProcessor.INTERNAL_ACCESS_TOKEN, 
					accessToken.getValue());
		} catch (WrongArgumentException e)
		{
			return makeBearerError(BearerTokenError.INVALID_TOKEN);
		}
		
		OAuthToken parsedAccessToken = parseInternalToken(internalAccessToken);
		String contents = parsedAccessToken.getUserInfo();
		if (contents == null)
			return makeBearerError(BearerTokenError.INSUFFICIENT_SCOPE);
		return toResponse(Response.ok(contents).header(HttpHeaders.CONTENT_TYPE, "application/json"));
	}
}
