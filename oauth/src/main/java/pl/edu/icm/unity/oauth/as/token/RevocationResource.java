/*
 * Copyright (c) 2015, Jirav All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as.token;

import java.util.Arrays;
import java.util.Optional;

import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.oauth.as.OAuthProcessor;
import pl.edu.icm.unity.oauth.as.OAuthToken;
import pl.edu.icm.unity.server.api.internal.LoginSession;
import pl.edu.icm.unity.server.api.internal.SessionManagement;
import pl.edu.icm.unity.server.api.internal.Token;
import pl.edu.icm.unity.server.api.internal.TokensManagement;
import pl.edu.icm.unity.types.authn.AuthenticationRealm;
import pl.edu.icm.unity.types.basic.EntityParam;

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
 * <p>
 * Unity also supports one non standard extension. If a logout=true parameter is added, then 
 * besides token revocation also the token's owner's session is killed. To allow for this,
 * a special OAuth scope must be associated with the token: 'single-logout'.
 * 
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
	public static final String LOGOUT = "logout";
	public static final String LOGOUT_SCOPE = "single-logout";
	
	private TokensManagement tokensManagement;
	private SessionManagement sessionManagement;
	private AuthenticationRealm realm;
	
	public RevocationResource(TokensManagement tokensManagement, SessionManagement sessionManagement, 
			AuthenticationRealm realm)
	{
		this.tokensManagement = tokensManagement;
		this.sessionManagement = sessionManagement;
		this.realm = realm;
	}

	@Path("/")
	@POST
	public Response revoke(@FormParam(TOKEN) String token, @FormParam(CLIENT) String clientId, 
			@FormParam(TOKEN_TYPE) String tokenHint, @FormParam(LOGOUT) String logout) 
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
		
		if ("true".equals(logout))
		{
			Response r = killSession(parsedAccessToken, internalAccessToken.getOwner());
			if (r != null)
				return r;
		}
		
		try
		{
			tokensManagement.removeToken(OAuthProcessor.INTERNAL_ACCESS_TOKEN, token);
		} catch (WrongArgumentException e)
		{
			//ok
		}
		return toResponse(Response.ok());
	}
	
	private Response killSession(OAuthToken parsedAccessToken, long entity) throws EngineException
	{
		if (parsedAccessToken.getScope() == null)
			return makeError(OAuth2Error.INVALID_SCOPE, "Insufficent scope to perform full logout.");
		Optional<String> logoutScope = Arrays.stream(parsedAccessToken.getScope()).
				filter(scope -> LOGOUT_SCOPE.equals(scope)).
				findAny();
		if (!logoutScope.isPresent())
			return makeError(OAuth2Error.INVALID_SCOPE, "Insufficent scope to perform full logout.");
		try
		{
			LoginSession ownedSession = sessionManagement.getOwnedSession(
					new EntityParam(entity), realm.getName());
			sessionManagement.removeSession(ownedSession.getId(), true);
		} catch (WrongArgumentException e)
		{
			//ok - no session
		}
		return null;
	}
}
