/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as.token;

import java.util.Optional;

import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.base.Joiner;
import com.nimbusds.jwt.util.DateUtils;
import com.nimbusds.oauth2.sdk.OAuth2Error;

import net.minidev.json.JSONObject;
import pl.edu.icm.unity.base.token.Token;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.oauth.as.OAuthToken;
import pl.edu.icm.unity.oauth.as.token.BaseTokenResource.TokensPair;

/**
 * Implementation of RFC 7662 - OAuth 2.0 Token Introspection. 
 * Similar to (older) {@link TokenInfoResource}, however standard, with more restricted authorization.
 * Currently only supports bearer tokens (refresh and access). May be enhanced to support signed openId tokens
 * but that makes little sense (those are intended for self validation).
 */
@Produces("application/json")
@Path(OAuthTokenEndpoint.TOKEN_INTROSPECTION_PATH)
public class TokenIntrospectionResource extends BaseOAuthResource
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_OAUTH, TokenIntrospectionResource.class);
	private final OAuthAccessTokenRepository accessTokenDAO;
	private final OAuthRefreshTokenRepository refreshTokenDAO;

	
	public TokenIntrospectionResource(OAuthAccessTokenRepository tokenDAO, 
			OAuthRefreshTokenRepository refreshTokenDAO)
	{
		this.accessTokenDAO = tokenDAO;
		this.refreshTokenDAO = refreshTokenDAO;
	}

	@Path("/")
	@POST
	public Response introspectToken(@FormParam("token") String token) 
			throws EngineException, JsonProcessingException
	{
		if (token == null)
			throw new OAuthErrorException(
					makeError(OAuth2Error.INVALID_REQUEST, "Token for introspection was not provided"));
		
		log.debug("Token introspection enquiry for token {}", tokenToLog(token));
		
		Optional<TokensPair> tokensOpt = loadToken(token);
		if (!tokensOpt.isPresent())
		{
			log.debug("Token {} is not present, returning inactive response", tokenToLog(token));
			return getOKResponse(getInactiveResponse());
		}
		
		TokensPair tokens = tokensOpt.get();
		
		return getOKResponse(getBearerStyleTokenInfo(tokens));
	}


	private Optional<TokensPair> loadToken(String token)
	{
		try
		{
			Token rawToken = accessTokenDAO.readAccessToken(token);
			OAuthToken parsedAccessToken = parseInternalToken(rawToken);
			return Optional.of(new TokensPair(rawToken, parsedAccessToken));
		} catch (IllegalArgumentException e)
		{
			try
			{
				Token rawToken = refreshTokenDAO.readRefreshToken(token);
				OAuthToken parsedAccessToken = parseInternalToken(rawToken);
				return Optional.of(new TokensPair(rawToken, parsedAccessToken));
			} catch (IllegalArgumentException e2)
			{
				return Optional.empty();
			}
		}
	}
	
	private JSONObject getInactiveResponse()
	{
		JSONObject ret = new JSONObject();
		ret.put("active", false);
		return ret;
	}

	private JSONObject getBearerStyleTokenInfo(TokensPair tokens)
	{
		JSONObject ret = new JSONObject();
		ret.put("active", true);
		ret.put("scope", Joiner.on(' ').join(tokens.parsedToken.getEffectiveScope()));
		ret.put("client_id", tokens.parsedToken.getClientUsername());
		ret.put("token_type", "bearer");
		ret.put("exp", DateUtils.toSecondsSinceEpoch(tokens.tokenSrc.getExpires()));
		ret.put("iat", DateUtils.toSecondsSinceEpoch(tokens.tokenSrc.getCreated()));
		ret.put("nbf", DateUtils.toSecondsSinceEpoch(tokens.tokenSrc.getCreated()));
		ret.put("sub", tokens.parsedToken.getSubject());
		ret.put("aud",
				tokens.parsedToken.getAudience() != null && tokens.parsedToken.getAudience().size() == 1
						? tokens.parsedToken.getAudience().get(0)
						: tokens.parsedToken.getAudience());
		ret.put("iss", tokens.parsedToken.getIssuerUri());
		log.debug("Returning token information: {}", ret.toJSONString());
		return ret;
	}
	
	private Response getOKResponse(JSONObject jsonObject)
	{
		return toResponse(Response.ok(jsonObject.toJSONString()));
	}
}
