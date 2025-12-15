/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.oauth.as.token.access;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nimbusds.jwt.JWT;
import com.nimbusds.oauth2.sdk.AccessTokenResponse;
import com.nimbusds.oauth2.sdk.GrantType;
import com.nimbusds.oauth2.sdk.OAuth2Error;
import com.nimbusds.oauth2.sdk.Scope;
import com.nimbusds.oauth2.sdk.id.Audience;
import com.nimbusds.oauth2.sdk.id.Issuer;
import com.nimbusds.oauth2.sdk.id.Subject;
import com.nimbusds.oauth2.sdk.token.AccessToken;
import com.nimbusds.oauth2.sdk.token.BearerAccessToken;
import com.nimbusds.oauth2.sdk.token.Tokens;
import com.nimbusds.openid.connect.sdk.Nonce;
import com.nimbusds.openid.connect.sdk.OIDCScopeValue;
import com.nimbusds.openid.connect.sdk.claims.IDTokenClaimsSet;
import com.nimbusds.openid.connect.sdk.claims.UserInfo;

import jakarta.ws.rs.core.Response;
import pl.edu.icm.unity.base.entity.Entity;
import pl.edu.icm.unity.base.entity.EntityParam;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.identity.IdentityTaV;
import pl.edu.icm.unity.base.identity.IllegalIdentityValueException;
import pl.edu.icm.unity.base.token.Token;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.attributes.DynamicAttribute;
import pl.edu.icm.unity.engine.api.authn.InvocationContext;
import pl.edu.icm.unity.engine.api.authn.RemoteAuthnMetadata;
import pl.edu.icm.unity.engine.api.authn.SerializableRemoteAuthnMetadata;
import pl.edu.icm.unity.engine.api.group.IllegalGroupValueException;
import pl.edu.icm.unity.engine.api.idp.EntityInGroup;
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
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;

class ExchangeTokenHandler
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_OAUTH, ExchangeTokenHandler.class);

	private final OAuthASProperties config;
	private final AccessTokenFactory accessTokenFactory;
	private final OAuthAccessTokenRepository accessTokensDAO;
	private final ClientAttributesProvider clientAttributesProvider;
	private final OAuthTokenStatisticPublisher statisticPublisher;
	private final EffectiveScopesAttributesCompleter oAuthTokenEffectiveScopesAttributesCompleter;
	private final EntityManagement idMan;
	private final OAuthIdPEngine notAuthorizedOauthIdpEngine;

	public ExchangeTokenHandler(OAuthASProperties config, AccessTokenFactory accessTokenFactory,
			OAuthAccessTokenRepository accessTokensDAO, OAuthTokenStatisticPublisher statisticPublisher,
			ClientAttributesProvider clientAttributesProvider,
			EffectiveScopesAttributesCompleter oAuthTokenEffectiveScopesAttributesCompleter, EntityManagement idMan,
			OAuthIdPEngine notAuthorizedOauthIdpEngine)
	{
		this.config = config;
		this.accessTokenFactory = accessTokenFactory;
		this.accessTokensDAO = accessTokensDAO;
		this.statisticPublisher = statisticPublisher;
		this.clientAttributesProvider = clientAttributesProvider;
		this.oAuthTokenEffectiveScopesAttributesCompleter = oAuthTokenEffectiveScopesAttributesCompleter;
		this.idMan = idMan;
		this.notAuthorizedOauthIdpEngine = notAuthorizedOauthIdpEngine;
	}

	Response handleExchangeToken(String subjectToken, String subjectTokenType, String requestedTokenType,
			List<String> audience, String scope, String actorToken, String actorTokenType, List<String> resource,
			String acceptHeader) throws JsonProcessingException, EngineException
	{

		long callerEntityId = InvocationContext.getCurrent()
				.getLoginSession()
				.getEntityId();

		String callerIdentity = InvocationContext.getCurrent()
				.getLoginSession()
				.getAuthenticatedIdentities()
				.iterator()
				.next();

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

		oAuthTokenEffectiveScopesAttributesCompleter.addAttributesToScopesDefinitionIfMissing(config,
				parsedSubjectToken);
		Scope newRequestedScopeList = getNewRequestedScopeWitoutTokenExchangeScope(scope);
		List<String> oldRequestedScopesList = Arrays.asList(parsedSubjectToken.getRequestedScope());

		try
		{
			validateExchangeRequest(subjectTokenType, requestedTokenType, oldRequestedScopesList, actorToken,
					actorTokenType);

			if (requestedTokenType == null || requestedTokenType.equals(AccessTokenResource.ACCESS_TOKEN_TYPE_ID))
			{
				return handldeExchangeToAccessToken(parsedSubjectToken, subToken.getOwner(), callerEntityId,
						callerIdentity, audience, newRequestedScopeList, actorToken, actorTokenType, resource,
						acceptHeader);
			} else if (requestedTokenType.equals(AccessTokenResource.ID_TOKEN_TYPE_ID))
			{

				return handleExchangeToIdToken(parsedSubjectToken, subToken.getOwner(), callerEntityId, callerIdentity,
						audience, newRequestedScopeList, actorToken, actorTokenType, resource);
			} else
			{
				return BaseOAuthResource.makeError(OAuth2Error.INVALID_REQUEST, "wrong requested token type");
			}

		} catch (OAuthErrorException e)
		{
			return e.response;
		}
	}

	private Response handldeExchangeToAccessToken(OAuthToken oldToken, Long tokenOwnerEntityId, long callerEntityId,
			String callerIdentity, List<String> audiences, Scope newRequestedScopeList, String actorToken,
			String actorTokenType, List<String> resource, String acceptHeader)
			throws OAuthErrorException, JsonProcessingException, EngineException

	{
		OAuthToken newToken = new OAuthToken(oldToken);
		List<String> filteredScopes = AttributeValueFilterUtils.getScopesWithoutFilterClaims(newRequestedScopeList)
				.stream()
				.map(v -> v.getValue())
				.toList();
		Map<String, RequestedOAuthScope> mappedScopes = mapToPreviouslyAssignedScopesWithPatternSupport(
				oldToken.getEffectiveScope(), filteredScopes);
		if (mappedScopes.values()
				.stream()
				.anyMatch(Objects::isNull))
		{
			return BaseOAuthResource.makeError(OAuth2Error.INVALID_SCOPE, null);
		}
		newToken.setRequestedScope(filteredScopes.toArray(String[]::new));

		List<RequestedOAuthScope> newValidScopes = buildNewEffectiveScopes(mappedScopes);
		newToken.setEffectiveScope(newValidScopes);

		SerializableRemoteAuthnMetadata serializableRemoteAuthnMetadata = oldToken.getRemoteIdPAuthnContext();

		TranslationResult userInfoRes = getAttributes(callerEntityId, tokenOwnerEntityId,
				GrantType.TOKEN_EXCHANGE.getValue(), Optional.ofNullable(serializableRemoteAuthnMetadata)
						.map(remoteMeta -> new RemoteAuthnMetadata(remoteMeta.protocol, remoteMeta.remoteIdPId,
								remoteMeta.classReferences))
						.orElse(null));

		List<AttributeFilteringSpec> mergedFilters = mergeFilters(newRequestedScopeList,
				oldToken.getAttributeValueFilters());
		newToken.setAttributeValueFilters(mergedFilters);

		UserInfo userInfoClaimSet = createUserInfo(newValidScopes, oldToken.getSubject(),
				oldToken.getRequestedACR() != null && !oldToken.getRequestedACR()
						.isEmpty(),
				userInfoRes, mergedFilters);
		newToken.setUserInfo(userInfoClaimSet.toJSONObject()
				.toJSONString());

		List<String> newAudience = Stream
				.concat(audiences == null ? Stream.empty() : audiences.stream(),
						resource == null ? Stream.empty() : resource.stream())
				.toList();

		newToken.setOpenidToken(null);
		newToken.setMaxExtendedValidity(config.getMaxExtendedAccessTokenValidity());
		newToken.setTokenValidity(config.getAccessTokenValidity());
		newToken.setRefreshToken(null);
		newToken.setIssuerUri(config.getIssuerName());
		newToken.setClientId(callerEntityId);
		newToken.setAudience(newAudience);
		newToken.setClientUsername(callerIdentity);
		newToken.setClientType(clientAttributesProvider.getClientType(new EntityParam(callerEntityId)));
		newToken.setClientName(clientAttributesProvider.getClientName(new EntityParam(callerEntityId)));

		Date now = new Date();

		AccessToken accessToken = accessTokenFactory.create(newToken, now, acceptHeader);
		newToken.setAccessToken(accessToken.getValue());
		Date accessExpiration = TokenUtils.getAccessTokenExpiration(config, now);

		Map<String, Object> additionalParams = new HashMap<>();
		additionalParams.put("issued_token_type", AccessTokenResource.ACCESS_TOKEN_TYPE_ID);

		AccessTokenResponse oauthResponse = new AccessTokenResponse(new Tokens(accessToken, null), additionalParams);

		accessTokensDAO.storeAccessToken(accessToken, newToken, new EntityParam(tokenOwnerEntityId), now,
				accessExpiration);
		statisticPublisher.reportSuccess(newToken.getClientUsername(), newToken.getClientName(),
				new EntityParam(tokenOwnerEntityId));

		return BaseOAuthResource.toResponse(Response.ok(BaseOAuthResource.getResponseContent(oauthResponse)));

	}

	private Response handleExchangeToIdToken(OAuthToken oldToken, long tokenOwnerEntityId, long callerEntityId,
			String callerIdentity, List<String> audiences, Scope newRequestedScopeList, String actorToken,
			String actorTokenType, List<String> resource) throws OAuthErrorException
	{

		validateExchangeToIdToken(callerEntityId, audiences);

		List<String> scopesWithoutFilters = AttributeValueFilterUtils
				.getScopesWithoutFilterClaims(newRequestedScopeList)
				.stream()
				.map(v -> v.getValue())
				.toList();
		if (!scopesWithoutFilters.contains(OIDCScopeValue.OPENID.getValue()))
		{
			return BaseOAuthResource.makeError(OAuth2Error.INVALID_SCOPE, null);
		}

		Map<String, RequestedOAuthScope> mappedScopes = mapToPreviouslyAssignedScopesWithPatternSupport(
				oldToken.getEffectiveScope(), scopesWithoutFilters);
		if (mappedScopes.values()
				.stream()
				.anyMatch(Objects::isNull))
		{
			return BaseOAuthResource.makeError(OAuth2Error.INVALID_SCOPE, null);
		}
		List<RequestedOAuthScope> newValidScopes = buildNewEffectiveScopes(mappedScopes);

		SerializableRemoteAuthnMetadata serializableRemoteAuthnMetadata = oldToken.getRemoteIdPAuthnContext();
		TranslationResult userInfoRes = getAttributes(callerEntityId, tokenOwnerEntityId,
				GrantType.TOKEN_EXCHANGE.getValue(), Optional.ofNullable(serializableRemoteAuthnMetadata)
						.map(remoteMeta -> new RemoteAuthnMetadata(remoteMeta.protocol, remoteMeta.remoteIdPId,
								remoteMeta.classReferences))
						.orElse(null));

		List<AttributeFilteringSpec> mergedFilters = mergeFilters(newRequestedScopeList,
				oldToken.getAttributeValueFilters());
		UserInfo userInfoClaimSet = createUserInfo(newValidScopes, oldToken.getSubject(),
				oldToken.getRequestedACR() != null && !oldToken.getRequestedACR()
						.isEmpty(),
				userInfoRes, mergedFilters);

		List<String> newAudience = Stream
				.concat((audiences == null || audiences.isEmpty()) ? Stream.of(callerIdentity) : audiences.stream(),
						resource == null ? Stream.empty() : resource.stream())
				.toList();
		Date now = new Date();

		String idToken = createIdToken(now, getNonceFromIdToken(oldToken), Audience.create(newAudience),
				oldToken.hasSupportAttributesInIdToken()
						.orElse(false) ? userInfoClaimSet : null,
				oldToken.getSubject(), oldToken.getAuthenticationTime());

		Map<String, Object> additionalParams = new HashMap<>();
		additionalParams.put("issued_token_type", AccessTokenResource.ID_TOKEN_TYPE_ID);
		AccessTokenResponse tokenResponse = new AccessTokenResponse(new Tokens(new BearerAccessToken(idToken,
				config.getAccessTokenValidity(), Scope.parse(newValidScopes.stream()
						.map(s -> s.scope())
						.toList())),
				null), additionalParams);

		String clientName = clientAttributesProvider.getClientName(new EntityParam(callerEntityId));
		statisticPublisher.reportSuccess(callerIdentity, clientName, new EntityParam(tokenOwnerEntityId));

		return BaseOAuthResource.toResponse(Response.ok(BaseOAuthResource.getResponseContent(tokenResponse)));

	}

	private void validateExchangeToIdToken(long callerEntityId, List<String> audiences) throws OAuthErrorException
	{
		if (audiences == null || audiences.isEmpty())
			return;

		List<Entity> resolvedAudienceEntities = new ArrayList<>();

		for (String audience : audiences)
		{
			try
			{
				EntityParam audienceEntity = new EntityParam(new IdentityTaV(UsernameIdentity.ID, audience));
				resolvedAudienceEntities.add(idMan.getEntity(audienceEntity));
			} catch (IllegalIdentityValueException e)
			{
				continue;
			} catch (EngineException e)
			{
				log.debug("Can not resolve audience entity: " + audience, e);
			}
		}

		if (!resolvedAudienceEntities.stream()
				.map(entity -> entity.getId())
				.anyMatch(eId -> eId == callerEntityId))
			throw new OAuthErrorException(BaseOAuthResource.makeError(OAuth2Error.INVALID_REQUEST, "wrong audience"));
	}

	private Nonce getNonceFromIdToken(OAuthToken token) throws OAuthErrorException
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
			log.error("Cannot parse IDToken claims", e);
			throw new OAuthErrorException(BaseOAuthResource.makeError(OAuth2Error.SERVER_ERROR, e.getMessage()));
		}
		return oldClaims.getNonce();
	}

	private String createIdToken(Date now, Nonce oldNonce, List<Audience> audience, UserInfo userInfoClaimSet,
			String subject, Instant authenticationTime) throws OAuthErrorException

	{
		IDTokenClaimsSet newClaims = new IDTokenClaimsSet(new Issuer(config.getIssuerName()), new Subject(subject),
				audience, TokenUtils.getAccessTokenExpiration(config, now), now);
		newClaims.setNonce(oldNonce);
		if (authenticationTime != null)
		{
			newClaims.setAuthenticationTime(Date.from(authenticationTime));
		}

		if (userInfoClaimSet != null)
		{
			newClaims.putAll(userInfoClaimSet);
		}
		try
		{
			return config.getTokenSigner()
					.sign(newClaims)
					.serialize();
		} catch (Exception e)
		{
			log.error("Cannot create new id token", e);
			throw new OAuthErrorException(BaseOAuthResource.makeError(OAuth2Error.SERVER_ERROR, e.getMessage()));
		}
	}

	private List<AttributeFilteringSpec> mergeFilters(Scope newRequestedScopeList,
			List<AttributeFilteringSpec> oldFilers)
	{
		List<AttributeFilteringSpec> claimFilters = AttributeValueFilterUtils
				.getFiltersFromScopes(newRequestedScopeList);

		return AttributeValueFilterUtils.mergeFiltersWithPreservingLast(oldFilers, claimFilters);
	}

	private UserInfo createUserInfo(List<RequestedOAuthScope> validScopes, String subject, boolean addAcr,
			TranslationResult userInfoRes, List<AttributeFilteringSpec> claimValueFilters)
	{
		Set<String> requestedAttributes = new HashSet<>();
		for (RequestedOAuthScope si : validScopes)
			requestedAttributes.addAll(si.scopeDefinition()
					.attributes());

		if (addAcr)
		{
			requestedAttributes.add(IDTokenClaimsSet.ACR_CLAIM_NAME);
		}

		Collection<DynamicAttribute> attributes = OAuthProcessor.filterAttributes(userInfoRes, requestedAttributes,
				claimValueFilters);

		return OAuthProcessor.prepareUserInfoClaimSet(subject, attributes);
	}

	private List<RequestedOAuthScope> buildNewEffectiveScopes(Map<String, RequestedOAuthScope> scopeMapping)
	{

		return scopeMapping.entrySet()
				.stream()
				.map(e -> new RequestedOAuthScope(e.getKey(), e.getValue()
						.scopeDefinition(), false))
				.toList();
	}

	private TranslationResult getAttributes(long clientId, long ownerId, String grant,
			RemoteAuthnMetadata remoteAuthnMetadata) throws OAuthErrorException
	{
		EntityInGroup client = new EntityInGroup(config.getValue(OAuthASProperties.CLIENTS_GROUP),
				new EntityParam(clientId));
		TranslationResult userInfoRes = null;

		try
		{
			userInfoRes = notAuthorizedOauthIdpEngine.getUserInfoUnsafe(ownerId, String.valueOf(clientId),
					Optional.of(client), config.getValue(OAuthASProperties.USERS_GROUP),
					config.getOutputTranslationProfile(), grant, config, remoteAuthnMetadata);
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

	private void validateExchangeRequest(String subjectTokenType, String requestedTokenType,
			List<String> oldRequestedScopesList, String actorToken, String actorTokenType) throws OAuthErrorException
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

		if (!oldRequestedScopesList.contains(AccessTokenResource.EXCHANGE_SCOPE))
		{
			throw new OAuthErrorException(BaseOAuthResource.makeError(OAuth2Error.UNAUTHORIZED_CLIENT,
					"subject_token must have  " + AccessTokenResource.EXCHANGE_SCOPE + " scope"));
		}
	}
}
