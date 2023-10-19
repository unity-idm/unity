/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as.token;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;

import org.apache.hc.core5.http.HttpHeaders;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nimbusds.oauth2.sdk.token.BearerTokenError;

import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.oauth.as.token.access.OAuthAccessTokenRepository;

/**
 * RESTful implementation of the user information token resource
 * 
 * @author K. Benedyczak
 */
@Produces("application/json")
@Path(OAuthTokenEndpoint.USER_INFO_PATH)
public class UserInfoResource extends BaseTokenResource
{
	public UserInfoResource(OAuthAccessTokenRepository tokensDAO)
	{
		super(tokensDAO);
	}
	
	@Path("/")
	@POST
	public Response getTokenViaPOST(@HeaderParam("Authorization") String bearerToken) 
			throws EngineException, JsonProcessingException
	{
		return getToken(bearerToken);
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
