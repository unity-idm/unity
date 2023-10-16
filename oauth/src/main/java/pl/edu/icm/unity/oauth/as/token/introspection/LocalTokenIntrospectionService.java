/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.oauth.as.token.introspection;

import java.util.Optional;

import jakarta.ws.rs.core.Response;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.base.Joiner;
import com.nimbusds.jwt.util.DateUtils;

import net.minidev.json.JSONObject;
import pl.edu.icm.unity.base.token.Token;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.oauth.as.OAuthToken;
import pl.edu.icm.unity.oauth.as.token.BaseOAuthResource;
import pl.edu.icm.unity.oauth.as.token.BaseTokenResource.TokensPair;
import pl.edu.icm.unity.oauth.as.token.access.OAuthAccessTokenRepository;
import pl.edu.icm.unity.oauth.as.token.access.OAuthRefreshTokenRepository;

class LocalTokenIntrospectionService
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_OAUTH, LocalTokenIntrospectionService.class);
	private final OAuthAccessTokenRepository accessTokenRespository;
	private final OAuthRefreshTokenRepository refreshTokenRepository;

	LocalTokenIntrospectionService(OAuthAccessTokenRepository accessTokenRespository,
			OAuthRefreshTokenRepository refreshTokenRepository)
	{
		this.accessTokenRespository = accessTokenRespository;
		this.refreshTokenRepository = refreshTokenRepository;
	}

	Response processLocalIntrospection(String token)
	{
		log.debug("Localy token introspection, token {}", BaseOAuthResource.tokenToLog(token));
		return loadToken(token).map(tokens -> getOKResponse(getBearerStyleTokenInfo(tokens)))
				.orElse(getTokenNotPresentResponse(token));

	}

	private Response getTokenNotPresentResponse(String token)
	{
		log.debug("Token {} is not present, returning inactive response", BaseOAuthResource.tokenToLog(token));
		return getOKResponse(TokenIntrospectionResource.getInactiveResponse());
	}
	
	private Optional<TokensPair> loadToken(String token)
	{
		try
		{
			Token rawToken = accessTokenRespository.readAccessToken(token);
			OAuthToken parsedAccessToken = BaseOAuthResource.parseInternalToken(rawToken);
			return Optional.of(new TokensPair(rawToken, parsedAccessToken));
		} catch (IllegalArgumentException e)
		{
			log.trace("Can not find access token", e);
			try
			{
				Token rawToken = refreshTokenRepository.readRefreshToken(token);
				OAuthToken parsedAccessToken = BaseOAuthResource.parseInternalToken(rawToken);
				return Optional.of(new TokensPair(rawToken, parsedAccessToken));
			} catch (IllegalArgumentException e2)
			{
				log.trace("Can not find refresh token", e);
				return Optional.empty();
			}
		}
	}

	private JSONObject getBearerStyleTokenInfo(TokensPair tokens)
	{
		OAuthToken parsedToken = tokens.parsedToken;
		
		JSONObject ret = new JSONObject();
		ret.put("active", true);
		ret.put("scope", Joiner.on(' ')
				.join(parsedToken.getEffectiveScope()));
		ret.put("client_id", parsedToken.getClientUsername());
		ret.put("token_type", "bearer");
		ret.put("exp", DateUtils.toSecondsSinceEpoch(tokens.tokenSrc.getExpires()));
		ret.put("iat", DateUtils.toSecondsSinceEpoch(tokens.tokenSrc.getCreated()));
		ret.put("nbf", DateUtils.toSecondsSinceEpoch(tokens.tokenSrc.getCreated()));
		ret.put("sub", parsedToken.getSubject());
		ret.put("aud", getTokenAudience(parsedToken));
		ret.put("iss", parsedToken.getIssuerUri());
		log.debug("Returning token information: {}", ret.toJSONString());
		return ret;
	}

	private Object getTokenAudience(OAuthToken parsedToken)
	{
		return parsedToken.getAudience() != null && parsedToken.getAudience()
				.size() == 1 ? parsedToken.getAudience()
						.get(0) : parsedToken.getAudience();
	}
	
	private Response getOKResponse(JSONObject jsonObject)
	{
		return BaseOAuthResource.toResponse(Response.ok(jsonObject.toJSONString()));
	}

	@Component
	public static class LocalTokenIntrospectionServiceFactory
	{
		private final OAuthAccessTokenRepository accessTokenRespository;
		private final OAuthRefreshTokenRepository refreshTokenRepository;

		@Autowired
		public LocalTokenIntrospectionServiceFactory(OAuthAccessTokenRepository accessTokenRespository,
				OAuthRefreshTokenRepository refreshTokenRepository)
		{

			this.accessTokenRespository = accessTokenRespository;
			this.refreshTokenRepository = refreshTokenRepository;
		}

		LocalTokenIntrospectionService getService()
		{
			return new LocalTokenIntrospectionService(accessTokenRespository, refreshTokenRepository);
		}

	}
}
