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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nimbusds.oauth2.sdk.token.BearerTokenError;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.api.internal.TokensManagement;

/**
 * RESTful implementation of the user information token resource
 * 
 * @author K. Benedyczak
 */
@Produces("application/json")
@Path(OAuthTokenEndpoint.USER_INFO_PATH)
public class UserInfoResource extends BaseTokenResource
{
	public UserInfoResource(TokensManagement tokensManagement)
	{
		super(tokensManagement);
	}

	@Path("/")
	@GET
	public Response getToken(@HeaderParam("Authorization") String bearerToken) 
			throws EngineException, JsonProcessingException
	{
		TokensPair internalAccessToken;
		try
		{
			internalAccessToken = super.resolveBearerToken(bearerToken);
			extendValidityIfNeeded(internalAccessToken.tokenSrc, internalAccessToken.parsedToken);
		} catch (OAuthTokenException e)
		{
			return e.getErrorResponse();
		}
		
		String contents = internalAccessToken.parsedToken.getUserInfo();
		if (contents == null)
			return makeBearerError(BearerTokenError.INSUFFICIENT_SCOPE);
		return toResponse(Response.ok(contents).header(HttpHeaders.CONTENT_TYPE, "application/json"));
	}
}
