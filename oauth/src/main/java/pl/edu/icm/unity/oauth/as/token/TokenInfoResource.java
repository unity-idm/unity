/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as.token;

import java.util.Date;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.Lists;
import com.nimbusds.jwt.util.DateUtils;

import net.minidev.json.JSONObject;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.token.TokensManagement;
import pl.edu.icm.unity.exceptions.EngineException;

/**
 * Non standard functionality: allows for validation of a given access token.
 * <p>
 * The request must be authorized with the bearer access token.
 * <p>
 * The successful response is the same as obtained when the access token is issued by the token endpoint.
 * If the token is invalid then the error Bearer Token error is returned. 
 * <p>
 * Successful response includes expiration date of the token, its scopes, associated user and client.
 * <code>
 * {
 *  "sub": "subject id",
 *  "client_id": "client id",
 *  "exp": "12345678",
 *  "scope": ["scope1", "scope2"]
 * }
 * </code>
 * 
 * @author K. Benedyczak
 */
@Produces("application/json")
@Path(OAuthTokenEndpoint.TOKEN_INFO_PATH)
public class TokenInfoResource extends BaseTokenResource
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_OAUTH, TokenInfoResource.class);
	
	public static final String SCOPE = "scope";
	public static final String EXPIRATION = "exp";
	public static final String SUBJECT = "sub";
	public static final String CLIENT = "client_id";
	public static final String AUDIENCE = "aud";
	
	public TokenInfoResource(TokensManagement tokensManagement)
	{
		super(tokensManagement);
	}

	@Path("/")
	@GET
	public Response getToken(@HeaderParam("Authorization") String bearerToken) 
			throws EngineException, JsonProcessingException
	{
		TokensPair tokens;
		try
		{
			tokens = super.resolveBearerToken(bearerToken);
			log.debug("Get token {} info returning status with expiration {}", 
					tokenToLog(bearerToken), tokens.tokenSrc.getExpires());
		} catch (OAuthTokenException e)
		{
			log.debug("Get token info returning error {} for token {}", 
					e.getErrorResponse().getStatusInfo(), tokenToLog(bearerToken));
			return e.getErrorResponse();
		}
		
		JSONObject contents = toJSON(tokens.parsedToken.getSubject(), tokens.parsedToken.getClientUsername(), 
				tokens.tokenSrc.getExpires(), 
				tokens.parsedToken.getEffectiveScope(), tokens.parsedToken.getAudience());
		return toResponse(Response.ok(contents.toJSONString()));
	}
	
	
	private JSONObject toJSON(String subject, String clientId, Date expiration, String[] scopes, String audience)
	{
		JSONObject ret = new JSONObject();
		ret.put(SUBJECT, subject);
		ret.put(CLIENT, clientId);
		ret.put(EXPIRATION, DateUtils.toSecondsSinceEpoch(expiration));
		if (scopes != null)
		{
			List<String> scopesAsList = Lists.newArrayList(scopes);
			ret.put(SCOPE, scopesAsList);
		}
		ret.put(AUDIENCE, audience);
		return ret;
	}
}
