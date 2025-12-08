/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.oauth.as.token.access;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nimbusds.oauth2.sdk.AccessTokenResponse;
import com.nimbusds.oauth2.sdk.GrantType;
import com.nimbusds.oauth2.sdk.OAuth2Error;
import com.nimbusds.oauth2.sdk.Scope;
import com.nimbusds.oauth2.sdk.token.AccessToken;
import com.nimbusds.oauth2.sdk.token.RefreshToken;

import jakarta.ws.rs.core.Response;
import pl.edu.icm.unity.base.entity.EntityParam;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.identity.IdentityTaV;
import pl.edu.icm.unity.base.identity.IllegalIdentityValueException;
import pl.edu.icm.unity.base.token.Token;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.authn.InvocationContext;
import pl.edu.icm.unity.oauth.as.OAuthASProperties;
import pl.edu.icm.unity.oauth.as.OAuthRequestValidator;
import pl.edu.icm.unity.oauth.as.OAuthToken;
import pl.edu.icm.unity.oauth.as.OAuthValidationException;
import pl.edu.icm.unity.oauth.as.token.BaseOAuthResource;
import pl.edu.icm.unity.oauth.as.token.OAuthErrorException;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;

class ExchangeTokenHandler
{	
	private static final Logger log = Log.getLogger(Log.U_SERVER_OAUTH, ExchangeTokenHandler.class);

	private final OAuthASProperties config;
	private final OAuthRefreshTokenRepository refreshTokensDAO;
	private final AccessTokenFactory accessTokenFactory;
	private final OAuthAccessTokenRepository accessTokensDAO;
	private final TokenService tokenService;
	private final ClientAttributesProvider clientAttributesProvider;
	private final OAuthTokenStatisticPublisher statisticPublisher;
	private final OAuthRequestValidator requestValidator;
	private final EntityManagement idMan;
	private final EffectiveScopesAttributesCompleter oAuthTokenEffectiveScopesAttributesCompleter;

	public ExchangeTokenHandler(OAuthASProperties config, OAuthRefreshTokenRepository refreshTokensDAO,
			AccessTokenFactory accessTokenFactory, OAuthAccessTokenRepository accessTokensDAO, TokenService tokenService,
			OAuthTokenStatisticPublisher statisticPublisher, OAuthRequestValidator requestValidator,
			EntityManagement idMan, ClientAttributesProvider clientAttributesProvider,
			EffectiveScopesAttributesCompleter oAuthTokenEffectiveScopesAttributesCompleter)
	{
		this.config = config;
		this.refreshTokensDAO = refreshTokensDAO;
		this.accessTokenFactory = accessTokenFactory;
		this.accessTokensDAO = accessTokensDAO;
		this.tokenService = tokenService;
		this.statisticPublisher = statisticPublisher;
		this.requestValidator = requestValidator;
		this.idMan = idMan;
		this.clientAttributesProvider = clientAttributesProvider;
		this.oAuthTokenEffectiveScopesAttributesCompleter = oAuthTokenEffectiveScopesAttributesCompleter;
	}

	Response handleExchangeToken(String subjectToken, String subjectTokenType, String requestedTokenType,
			String audience, String scope, String actorToken, String actorTokenType, List<String> resource,
			String acceptHeader) throws EngineException, JsonProcessingException
	{

		long callerEntityId = InvocationContext.getCurrent().getLoginSession().getEntityId();
		EntityParam audienceEntity = new EntityParam(new IdentityTaV(UsernameIdentity.ID, audience));

		Token subToken = null;
		OAuthToken parsedSubjectToken = null;

		try
		{
			subToken = accessTokensDAO.readAccessToken(subjectToken);
			parsedSubjectToken = BaseOAuthResource.parseInternalToken(subToken);
		} catch (Exception e)
		{
			return BaseOAuthResource.makeError(OAuth2Error.INVALID_REQUEST, "wrong subject_token");
		}

		oAuthTokenEffectiveScopesAttributesCompleter.addAttributesToScopesDefinitionIfMissing(config, parsedSubjectToken);
		
		Scope newRequestedScopeList = getNewRequestedScopeWitoutTokenExchangeScope(scope);
		
		List<String> oldRequestedScopesList = Arrays.asList(parsedSubjectToken.getRequestedScope());

		try
		{
			validateExchangeRequest(subjectTokenType, requestedTokenType, audience, callerEntityId, audienceEntity,
					oldRequestedScopesList, actorToken, actorTokenType);
		} catch (OAuthErrorException e)
		{
			return e.response;
		}

		List<String> newAudience = 
			    Stream.concat(Stream.of(audience),
			                  resource == null ? Stream.empty() : resource.stream())
			          .toList();
			
		
		OAuthToken newToken = null;
		try
		{
			newToken = tokenService.prepareTokenForExchange(parsedSubjectToken, newRequestedScopeList, oldRequestedScopesList,
					subToken.getOwner(), callerEntityId, newAudience,
					requestedTokenType != null && requestedTokenType.equals(AccessTokenResource.ID_TOKEN_TYPE_ID),
					GrantType.TOKEN_EXCHANGE.getValue());
		} catch (OAuthErrorException e)
		{
			return e.response;
		}

		newToken.setClientId(callerEntityId);
		newToken.setAudience(newAudience);
		newToken.setClientUsername(audience);
		newToken.setClientType(parsedSubjectToken.getClientType());

		try
		{
			newToken.setClientName(clientAttributesProvider.getClientName(audienceEntity));
		} catch (OAuthErrorException e)
		{
			return e.response;
		}

		Date now = new Date();
		AccessToken accessToken = accessTokenFactory.create(newToken, now, acceptHeader);
		newToken.setAccessToken(accessToken.getValue());

		RefreshToken refreshToken = refreshTokensDAO
				.createRefreshToken(config, now, newToken, subToken.getOwner()).orElse(null);
		Date accessExpiration = TokenUtils.getAccessTokenExpiration(config, now);

		Map<String, Object> additionalParams = new HashMap<>();
		additionalParams.put("issued_token_type", AccessTokenResource.ACCESS_TOKEN_TYPE_ID);

		AccessTokenResponse oauthResponse = tokenService.getAccessTokenResponse(newToken, accessToken, refreshToken,
				additionalParams);
		accessTokensDAO.storeAccessToken(accessToken, newToken, new EntityParam(subToken.getOwner()), now,
				accessExpiration);
		statisticPublisher.reportSuccess(parsedSubjectToken.getClientUsername(), parsedSubjectToken.getClientName(),
				new EntityParam(subToken.getOwner()));

		return BaseOAuthResource.toResponse(Response.ok(BaseOAuthResource.getResponseContent(oauthResponse)));
	}

	private Scope getNewRequestedScopeWitoutTokenExchangeScope(String scope)
	{
		Scope newRequestedScopeList = new Scope();
		if (scope != null && !scope.isEmpty())
		{
			newRequestedScopeList = Scope.parse(scope);
			if (newRequestedScopeList.contains(new Scope.Value(AccessTokenResource.EXCHANGE_SCOPE)))
			{
				log.debug("Removing unsupported {} scope from requested scopes list",
						AccessTokenResource.EXCHANGE_SCOPE);
				newRequestedScopeList.remove(new Scope.Value(AccessTokenResource.EXCHANGE_SCOPE));
			}
		}
		return newRequestedScopeList;
	}
	
	private void validateExchangeRequest(String subjectTokenType, String requestedTokenType, String audience,
			long callerEntityId, EntityParam audienceEntity, List<String> oldRequestedScopesList, String actorToken,
			String actorTokenType)
			throws OAuthErrorException
	{
		if (!subjectTokenType.equals(AccessTokenResource.ACCESS_TOKEN_TYPE_ID))
		{
			throw new OAuthErrorException(
					BaseOAuthResource.makeError(OAuth2Error.INVALID_REQUEST, "unsupported subject_token_type"));
		}

		if (requestedTokenType != null)
		{
			if (!(requestedTokenType.equals(AccessTokenResource.ACCESS_TOKEN_TYPE_ID)
					|| requestedTokenType.equals(AccessTokenResource.ID_TOKEN_TYPE_ID)))
			{
				throw new OAuthErrorException(
						BaseOAuthResource.makeError(OAuth2Error.INVALID_REQUEST, "unsupported requested_token_type"));
			}
		}
		
		if (StringUtils.isNoneEmpty(actorToken))
		{
			throw new OAuthErrorException(
					BaseOAuthResource.makeError(OAuth2Error.INVALID_REQUEST, "unsupported actor_token"));
		}
				
		if (StringUtils.isNoneEmpty(actorTokenType))
		{
			throw new OAuthErrorException(
					BaseOAuthResource.makeError(OAuth2Error.INVALID_REQUEST, "unsupported actor_token_type"));
		}

		try
		{
			idMan.getEntity(audienceEntity);
			requestValidator.validateGroupMembership(audienceEntity, audience);

		} catch (IllegalIdentityValueException | OAuthValidationException oe)
		{
			throw new OAuthErrorException(BaseOAuthResource.makeError(OAuth2Error.INVALID_REQUEST, "wrong audience"));
		} catch (EngineException e)
		{
			throw new OAuthErrorException(BaseOAuthResource.makeError(OAuth2Error.SERVER_ERROR,
					"Internal error, can not retrieve OAuth client's data"));
		}

		if (!oldRequestedScopesList.contains(AccessTokenResource.EXCHANGE_SCOPE))
		{
			throw new OAuthErrorException(BaseOAuthResource.makeError(OAuth2Error.UNAUTHORIZED_CLIENT,
					"subject_token must have  " + AccessTokenResource.EXCHANGE_SCOPE + " scope"));
		}
	}
}
