/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.oauth.as.token.access;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jwt.JWT;
import com.nimbusds.oauth2.sdk.AccessTokenResponse;
import com.nimbusds.oauth2.sdk.OAuth2Error;
import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.ResponseType;
import com.nimbusds.oauth2.sdk.Scope;
import com.nimbusds.oauth2.sdk.id.Audience;
import com.nimbusds.oauth2.sdk.id.Issuer;
import com.nimbusds.oauth2.sdk.id.Subject;
import com.nimbusds.oauth2.sdk.token.AccessToken;
import com.nimbusds.oauth2.sdk.token.RefreshToken;
import com.nimbusds.oauth2.sdk.token.Tokens;
import com.nimbusds.openid.connect.sdk.OIDCResponseTypeValue;
import com.nimbusds.openid.connect.sdk.OIDCScopeValue;
import com.nimbusds.openid.connect.sdk.OIDCTokenResponse;
import com.nimbusds.openid.connect.sdk.claims.IDTokenClaimsSet;
import com.nimbusds.openid.connect.sdk.claims.UserInfo;
import com.nimbusds.openid.connect.sdk.token.OIDCTokens;

import pl.edu.icm.unity.base.entity.EntityParam;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.exceptions.InternalException;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.attributes.DynamicAttribute;
import pl.edu.icm.unity.engine.api.group.IllegalGroupValueException;
import pl.edu.icm.unity.engine.api.idp.EntityInGroup;
import pl.edu.icm.unity.engine.api.idp.IdPEngine;
import pl.edu.icm.unity.engine.api.translation.ExecutionFailException;
import pl.edu.icm.unity.engine.api.translation.out.TranslationResult;
import pl.edu.icm.unity.oauth.as.AttributeFilteringSpec;
import pl.edu.icm.unity.oauth.as.AttributeValueFilterUtils;
import pl.edu.icm.unity.oauth.as.OAuthASProperties;
import pl.edu.icm.unity.oauth.as.OAuthProcessor;
import pl.edu.icm.unity.oauth.as.OAuthToken;
import pl.edu.icm.unity.oauth.as.RequestedOAuthScope;
import pl.edu.icm.unity.oauth.as.ScopeMatcher;
import pl.edu.icm.unity.oauth.as.token.BaseOAuthResource;
import pl.edu.icm.unity.oauth.as.token.OAuthErrorException;
import pl.edu.icm.unity.oauth.as.webauthz.OAuthIdPEngine;

class TokenService
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_OAUTH, TokenService.class);

	private final OAuthASProperties config;
	private final OAuthIdPEngine notAuthorizedOauthIdpEngine;

	TokenService( OAuthASProperties config,
			OAuthIdPEngine notAuthorizedOauthIdpEngine)
	{
		this.config = config;
		this.notAuthorizedOauthIdpEngine = notAuthorizedOauthIdpEngine;
	}

	OAuthToken prepareNewTokenBasedOnOldTokenForTokenExchange(
	        OAuthToken oldToken, Scope newRequestedScopeList, List<String> oldRequestedScopesList,
	        long ownerId, long clientId, List<String> audience,
	        boolean createIdToken, String grant) throws OAuthErrorException {

	    return prepareNewTokenBasedOnOldToken(
	            oldToken,
	            newRequestedScopeList,
	            ownerId,
	            clientId,
	            audience,
	            createIdToken,
	            grant,
	            this::mapToPreviouslyAssignedScopesWithPatternSupport);
	}

	OAuthToken prepareNewTokenBasedOnOldTokenForTokenRefresh(
	        OAuthToken oldToken, Scope newRequestedScopeList, List<String> oldRequestedScopesList,
	        long ownerId, long clientId, List<String> audience,
	        boolean createIdToken, String grant) throws OAuthErrorException {

	    return prepareNewTokenBasedOnOldToken(
	            oldToken,
	            newRequestedScopeList,
	            ownerId,
	            clientId,
	            audience,
	            createIdToken,
	            grant,
	            this::mapToPreviouslyAssignedScopesWithoutPatternSupport);
	}
	
	private OAuthToken prepareNewTokenBasedOnOldToken(
	        OAuthToken oldToken,
	        Scope newRequestedScopeList,
	        long ownerId,
	        long clientId,
	        List<String> audience,
	        boolean createIdToken,
	        String grant,
	        BiFunction<List<RequestedOAuthScope>, List<String>, Map<String, RequestedOAuthScope>> scopeMapper
	) throws OAuthErrorException {

	    OAuthToken newToken = new OAuthToken(oldToken);

	    List<String> filteredScopes = extractScopesWithoutFilterClaims(newRequestedScopeList);
	    Map<String, RequestedOAuthScope> scopeMapping =
	            validateAndMapScopes(oldToken, filteredScopes, scopeMapper);

	    newToken.setRequestedScope(filteredScopes.toArray(String[]::new));

	    TranslationResult userInfoRes = getAttributes(clientId, ownerId, grant);
	    List<RequestedOAuthScope> newValidScopes = buildNewEffectiveScopes(scopeMapping);

	    newToken.setEffectiveScope(newValidScopes);

	    UserInfo userInfoClaimSet =
	            updateUserInfoAndFilters(newToken, newRequestedScopeList, newValidScopes, userInfoRes);

	    handleIdTokenIfNeeded(newToken, filteredScopes, audience, createIdToken, userInfoClaimSet);

	    applyTokenMetadata(newToken);

	    return newToken;
	}


	private List<String> extractScopesWithoutFilterClaims(Scope newRequestedScopeList)
	{
		return AttributeValueFilterUtils.getScopesWithoutFilterClaims(newRequestedScopeList)
				.stream()
				.map(v -> v.getValue())
				.toList();
	}

	private Map<String, RequestedOAuthScope> validateAndMapScopes(OAuthToken oldToken, List<String> filteredScopes,
			BiFunction<List<RequestedOAuthScope>, List<String>, Map<String, RequestedOAuthScope>> mapper)
			throws OAuthErrorException
	{

		Map<String, RequestedOAuthScope> map = mapper.apply(oldToken.getEffectiveScope(), filteredScopes);

		for (RequestedOAuthScope v : map.values())
		{
			if (v == null)
			{
				throw new OAuthErrorException(BaseOAuthResource.makeError(OAuth2Error.INVALID_SCOPE, "wrong scope"));
			}
		}
		return map;
	}

	private List<RequestedOAuthScope> buildNewEffectiveScopes(Map<String, RequestedOAuthScope> scopeMapping)
	{

		return scopeMapping.entrySet()
				.stream()
				.map(e -> new RequestedOAuthScope(e.getKey(), e.getValue()
						.scopeDefinition(), false))
				.toList();
	}

	private UserInfo updateUserInfoAndFilters(OAuthToken newToken, Scope newRequestedScopeList,
			List<RequestedOAuthScope> newValidScopes, TranslationResult userInfoRes)
	{

		List<AttributeFilteringSpec> claimFilters = AttributeValueFilterUtils
				.getFiltersFromScopes(newRequestedScopeList);

		List<AttributeFilteringSpec> mergedFilters = AttributeValueFilterUtils
				.mergeFiltersWithPreservingLast(newToken.getAttributeValueFilters(), claimFilters);

		UserInfo userInfoClaimSet = createUserInfo(newValidScopes, newToken.getSubject(), userInfoRes, mergedFilters);

		newToken.setUserInfo(userInfoClaimSet.toJSONObject()
				.toJSONString());
		newToken.setAttributeValueFilters(mergedFilters);

		return userInfoClaimSet;
	}

	private void handleIdTokenIfNeeded(OAuthToken newToken, List<String> filteredScopes, List<String> audience,
			boolean createIdToken, UserInfo userInfoClaimSet) throws OAuthErrorException
	{

		boolean openid = filteredScopes.contains(OIDCScopeValue.OPENID.getValue());

		if (!openid || !createIdToken)
		{
			newToken.setOpenidToken(null);
			return;
		}

		try
		{
			Date now = new Date();
			newToken.setOpenidToken(createIdToken(now, newToken, Audience.create(audience), userInfoClaimSet));
		} catch (Exception e)
		{
			log.error("Cannot create new id token", e);
			throw new OAuthErrorException(BaseOAuthResource.makeError(OAuth2Error.SERVER_ERROR, e.getMessage()));
		}
	}

	private void applyTokenMetadata(OAuthToken newToken)
	{
		newToken.setMaxExtendedValidity(config.getMaxExtendedAccessTokenValidity());
		newToken.setTokenValidity(config.getAccessTokenValidity());
		newToken.setAccessToken(null);
		newToken.setRefreshToken(null);
		newToken.setIssuerUri(config.getIssuerName());
	}

	private Map<String, RequestedOAuthScope> mapToPreviouslyAssignedScopesWithPatternSupport(
			List<RequestedOAuthScope> previouslyAssigned, List<String> newlyRequested)
	{
		Map<String, RequestedOAuthScope> result = new HashMap<>();

		Map<String, RequestedOAuthScope> exactLookup = new HashMap<>();
		List<RequestedOAuthScope> patternScopes = new ArrayList<>();

		for (RequestedOAuthScope s : previouslyAssigned)
		{
			if (s.pattern())
			{
				patternScopes.add(s);
			} else
			{
				exactLookup.putIfAbsent(s.scope(), s);
			}
		}

		for (String req : newlyRequested)
		{
			RequestedOAuthScope exact = exactLookup.get(req);

			if (exact != null)
			{
				result.put(req, exact);
				continue;
			}

			for (RequestedOAuthScope w : patternScopes)
			{
				if (ScopeMatcher.isSubsetOfPatternScope(req, w.scope()))
				{
					result.put(req, w);
					break;
				}

			}

			result.putIfAbsent(req, null);
		}

		return result;
	}

	private Map<String, RequestedOAuthScope> mapToPreviouslyAssignedScopesWithoutPatternSupport(
			List<RequestedOAuthScope> previouslyAssigned, List<String> newlyRequested)
	{
		Map<String, RequestedOAuthScope> result = new HashMap<>();

		Map<String, RequestedOAuthScope> exactLookup = new HashMap<>();
		for (RequestedOAuthScope s : previouslyAssigned)     
		{
			exactLookup.putIfAbsent(s.scope(), s);
		}

		for (String req : newlyRequested)
		{
			RequestedOAuthScope exact = exactLookup.get(req);

			if (exact != null)
			{
				result.put(req, exact);
				continue;
			}

			result.putIfAbsent(req, null);
		}

		return result;
	}
	
	AccessTokenResponse getAccessTokenResponse(OAuthToken internalToken, AccessToken accessToken,
			RefreshToken refreshToken, Map<String, Object> additionalParams)
	{
		JWT signedJWT = TokenUtils.decodeIDToken(internalToken);
		AccessTokenResponse oauthResponse = signedJWT == null
				? new AccessTokenResponse(new Tokens(accessToken, refreshToken), additionalParams)
				: new OIDCTokenResponse(new OIDCTokens(signedJWT, accessToken, refreshToken), additionalParams);
		return oauthResponse;
	}

	private TranslationResult getAttributes(long clientId, long ownerId, String grant) throws OAuthErrorException
	{
		EntityInGroup client = new EntityInGroup(config.getValue(OAuthASProperties.CLIENTS_GROUP),
				new EntityParam(clientId));
		TranslationResult userInfoRes = null;

		try
		{
			userInfoRes = notAuthorizedOauthIdpEngine.getUserInfoUnsafe(ownerId, String.valueOf(clientId),
					Optional.of(client), config.getValue(OAuthASProperties.USERS_GROUP),
					config.getOutputTranslationProfile(), grant, config);
		} catch (ExecutionFailException e)
		{
			log.debug("Authentication failed due to profile's decision, returning error");
			throw new OAuthErrorException(BaseOAuthResource.makeError(OAuth2Error.ACCESS_DENIED, e.getMessage()));
		} catch (IllegalGroupValueException e)
		{
			log.warn("Entity trying to access OAuth resource is not a member of required group");
			throw new OAuthErrorException(BaseOAuthResource.makeError(OAuth2Error.ACCESS_DENIED, e.getMessage()));
		} catch (Exception e)
		{
			log.error("Engine problem when handling client request", e);
			throw new OAuthErrorException(BaseOAuthResource.makeError(OAuth2Error.SERVER_ERROR, e.getMessage()));
		}
		return userInfoRes;
	}

	private UserInfo createUserInfo(List<RequestedOAuthScope> validScopes, String userIdentity, TranslationResult userInfoRes,
			List<AttributeFilteringSpec> claimValueFilters)
	{
		Set<String> requestedAttributes = new HashSet<>();
		for (RequestedOAuthScope si : validScopes)
			requestedAttributes.addAll(si.scopeDefinition().attributes());

		Collection<DynamicAttribute> attributes = 
				OAuthProcessor.filterAttributes(userInfoRes, requestedAttributes, claimValueFilters);
		return OAuthProcessor.prepareUserInfoClaimSet(userIdentity, attributes);
	}

	private String createIdToken(Date now, OAuthToken token, List<Audience> audience, UserInfo userInfoClaimSet)
			throws ParseException, JOSEException, EngineException
	{
		JWT signedJWT = BaseOAuthResource.decodeIDToken(token);

		if (signedJWT == null)
			return null;

		IDTokenClaimsSet oldClaims;
		try
		{
			oldClaims = new IDTokenClaimsSet(signedJWT.getJWTClaimsSet());
		} catch (Exception e)
		{
			throw new InternalException("Can not parse the internal id token", e);
		}
		IDTokenClaimsSet newClaims = new IDTokenClaimsSet(new Issuer(config.getIssuerName()),
				new Subject(token.getSubject()), audience, TokenUtils.getAccessTokenExpiration(config, now), now);
		newClaims.setNonce(oldClaims.getNonce());

		if (token.hasSupportAttributesInIdToken().isPresent())
		{
			if (token.hasSupportAttributesInIdToken().get())
			{
				newClaims.putAll(userInfoClaimSet);
			}	
		} else
		{
			ResponseType responseType = null;
			if (StringUtils.isNoneEmpty(token.getResponseType()))
			{
				responseType = ResponseType.parse(token.getResponseType());
				if (responseType.contains(OIDCResponseTypeValue.ID_TOKEN) && responseType.size() == 1)
					newClaims.putAll(userInfoClaimSet);
			}
		}
		return config.getTokenSigner().sign(newClaims).serialize();
	}

	@Component
	public static class TokenServiceFactory
	{
		private final IdPEngine idPEngine;

		@Autowired
		public TokenServiceFactory(
				@Qualifier("insecure") IdPEngine idPEngine)
		{
			this.idPEngine = idPEngine;
		}

		public TokenService getTokenService(OAuthASProperties config)
		{
			return new TokenService( config,
					new OAuthIdPEngine(idPEngine));

		}
	}

}
