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

import org.apache.log4j.Logger;
import org.eclipse.jetty.http.HttpStatus;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.oauth.as.OAuthProcessor;
import pl.edu.icm.unity.oauth.as.OAuthToken;
import pl.edu.icm.unity.server.api.internal.Token;
import pl.edu.icm.unity.server.api.internal.TokensManagement;
import pl.edu.icm.unity.server.utils.Log;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nimbusds.jwt.JWT;
import com.nimbusds.oauth2.sdk.ErrorObject;
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
	private static final Logger log = Log.getLogger(Log.U_SERVER_OAUTH, UserInfoResource.class);
	
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
		
		JWT signedJWT = decodeIDToken(parsedAccessToken);
		try
		{	String contents = signedJWT.getJWTClaimsSet().toJSONObject().toJSONString();
			return toResponse(Response.ok(contents));
		} catch (java.text.ParseException e)
		{
			ErrorObject internal = new ErrorObject(null, "desc", HttpStatus.INTERNAL_SERVER_ERROR_500, null);
			log.error("Can't parse the JWT to JSON from the token", e);
			return makeError(internal, "Internal error decoding authorization token");
		}
	}
}
