/*
 * Copyright (c) 2015, Jirav All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as.token;

import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.oauth.as.OAuthProcessor;
import pl.edu.icm.unity.oauth.as.OAuthToken;
import pl.edu.icm.unity.server.api.internal.Token;
import pl.edu.icm.unity.server.api.internal.TokensManagement;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nimbusds.oauth2.sdk.ErrorObject;
import com.nimbusds.oauth2.sdk.OAuth2Error;
import com.nimbusds.oauth2.sdk.http.HTTPResponse;

/**
 * Implementation of RFC 7009  https://tools.ietf.org/html/rfc7009
 * <p>
 * Limitations: refresh tokens revocation is not implemented (as Unity doesn't support yet
 * refresh tokens at all). The endpoint access is not authorized - or better said the access
 * is authorized implicitly by providing a valid access token to be revoked. The user_id must be always given. 
 * <p>
 * Typical usage:
 * <code>
 * POST /.../revoke HTTP/1.1
   Host: ... 
   Content-Type: application/x-www-form-urlencoded
   
   token=45ghiukldjahdnhzdauz&user_id=oauth-client
 * </code>
 * 
 * @author K. Benedyczak
 */
@Produces("application/json")
@Path(OAuthTokenEndpoint.TOKEN_REVOCATION_PATH)
public class RevocationResource extends BaseOAuthResource
{
	public static final String TOKEN_TYPE = "token_type_hint";
	public static final String TOKEN_TYPE_AC = "access_token";
	public static final String UNSUPPORTED_TOKEN_TYPE_ERROR = "unsupported_token_type";
	public static final String TOKEN = "token";
	public static final String CLIENT = "client_id";
	
	private TokensManagement tokensManagement;
	
	public RevocationResource(TokensManagement tokensManagement)
	{
		this.tokensManagement = tokensManagement;
	}

	@Path("/")
	@POST
	public Response revoke(@FormParam(TOKEN) String token, @FormParam(CLIENT) String clientId, 
			@FormParam(TOKEN_TYPE) String tokenHint) 
			throws EngineException, JsonProcessingException
	{
		if (token == null)
			return makeError(OAuth2Error.INVALID_REQUEST, "To access the token revocation endpoint "
					+ "a token must be provided");
		if (clientId == null)
			return makeError(OAuth2Error.INVALID_REQUEST, "To access the token revocation endpoint "
					+ "a " + CLIENT + " must be provided");
		
		if (tokenHint != null && !TOKEN_TYPE_AC.equals(tokenHint))
			return makeError(new ErrorObject(UNSUPPORTED_TOKEN_TYPE_ERROR, "Invalid request", 
					HTTPResponse.SC_BAD_REQUEST), 
					"Only " + TOKEN_TYPE_AC + " type of token is supported");
		
		Token internalAccessToken;
		try
		{
			internalAccessToken = tokensManagement.getTokenById(OAuthProcessor.INTERNAL_ACCESS_TOKEN, 
					token);
		} catch (WrongArgumentException e)
		{
			return toResponse(Response.ok());
		}
		
		OAuthToken parsedAccessToken = parseInternalToken(internalAccessToken);
		
		if (!clientId.equals(parsedAccessToken.getClientUsername()))
			return makeError(OAuth2Error.INVALID_CLIENT, "Wrong client/token");
				
		try
		{
			tokensManagement.removeToken(OAuthProcessor.INTERNAL_ACCESS_TOKEN, token);
		} catch (WrongArgumentException e)
		{
			//ok
		}
		return toResponse(Response.ok());
	}
}
