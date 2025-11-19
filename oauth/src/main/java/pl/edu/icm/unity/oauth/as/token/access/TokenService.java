/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.oauth.as.token.access;

import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

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
import pl.edu.icm.unity.oauth.as.OAuthRequestValidator;
import pl.edu.icm.unity.oauth.as.OAuthRequestValidator.OAuthRequestValidatorFactory;
import pl.edu.icm.unity.oauth.as.OAuthToken;
import pl.edu.icm.unity.oauth.as.RequestedOAuthScope;
import pl.edu.icm.unity.oauth.as.token.BaseOAuthResource;
import pl.edu.icm.unity.oauth.as.token.OAuthErrorException;
import pl.edu.icm.unity.oauth.as.token.access.ClientAttributesProvider.ClientAttributesProviderFactory;
import pl.edu.icm.unity.oauth.as.webauthz.OAuthIdPEngine;

class TokenService
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_OAUTH, TokenService.class);

	private final OAuthRequestValidator requestValidator;
	private final OAuthASProperties config;
	private final OAuthIdPEngine notAuthorizedOauthIdpEngine;
	private final ClientAttributesProvider clientAttributesProvider;

	TokenService(OAuthRequestValidator requestValidator, OAuthASProperties config,
			OAuthIdPEngine notAuthorizedOauthIdpEngine, ClientAttributesProvider clientAttributesProvider)
	{
		this.requestValidator = requestValidator;
		this.config = config;
		this.notAuthorizedOauthIdpEngine = notAuthorizedOauthIdpEngine;
		this.clientAttributesProvider = clientAttributesProvider;
	}

	OAuthToken prepareNewTokenBasedOnOldToken(OAuthToken oldToken, String scope, List<String> oldRequestedScopesList,
			long ownerId, long clientId, String clientUserName, boolean createIdToken, String grant)
			throws OAuthErrorException
	{
		OAuthToken newToken = new OAuthToken(oldToken);
		
		Scope newRequestedScopeList = new Scope();
		if (scope != null && !scope.isEmpty())
		{
			newRequestedScopeList = Scope.parse(scope);
		}
		
		List<String> filteredScopes = AttributeValueFilterUtils.getScopesWithoutFilterClaims(newRequestedScopeList).stream().map(v -> v.getValue()).toList();
		
		if (!oldRequestedScopesList.containsAll(filteredScopes))
		{
			throw new OAuthErrorException(BaseOAuthResource.makeError(OAuth2Error.INVALID_SCOPE, "wrong scope"));
		}
		newToken.setRequestedScope(filteredScopes.stream().toArray(String[]::new));

		// get new attributes for identity
		TranslationResult userInfoRes = getAttributes(clientId, ownerId, grant);

		List<RequestedOAuthScope> newValidRequestedScopes = requestValidator.getValidRequestedScopes(
				clientAttributesProvider.getClientAttributes(new EntityParam(clientId)),
				AttributeValueFilterUtils.getScopesWithoutFilterClaims(newRequestedScopeList));
		newToken.setEffectiveScope(newValidRequestedScopes.stream().map(s -> s.scope()).toArray(String[]::new));

		List<AttributeFilteringSpec> claimFiltersFromScopes = AttributeValueFilterUtils.getFiltersFromScopes(newRequestedScopeList);
		List<AttributeFilteringSpec> mergedFilters = AttributeValueFilterUtils.mergeFiltersWithPreservingLast(newToken.getAttributeValueFilters(), claimFiltersFromScopes);
		UserInfo userInfoClaimSet = createUserInfo(newValidRequestedScopes, newToken.getSubject(), userInfoRes, mergedFilters);
		newToken.setUserInfo(userInfoClaimSet.toJSONObject().toJSONString());
		newToken.setAttributeValueFilters(mergedFilters);
		
		Date now = new Date();
		// if openid mode build new id_token using new userinfo.
		if (filteredScopes.contains(OIDCScopeValue.OPENID.getValue()) && createIdToken)
		{
			try
			{
				newToken.setOpenidToken(
						createIdToken(now, newToken, Arrays.asList(new Audience(clientUserName)), userInfoClaimSet));
			} catch (Exception e)
			{
				log.error("Cannot create new id token", e);
				throw new OAuthErrorException(BaseOAuthResource.makeError(OAuth2Error.SERVER_ERROR, e.getMessage()));
			}
		} else
		{
			// clear openidToken
			newToken.setOpenidToken(null);
		}

		newToken.setMaxExtendedValidity(config.getMaxExtendedAccessTokenValidity());
		newToken.setTokenValidity(config.getAccessTokenValidity());
		newToken.setAccessToken(null);
		newToken.setRefreshToken(null);
		newToken.setIssuerUri(config.getIssuerName());
		// responseType in newToken is the same as in oldToken
		// subject in newToken is the same as in oldToken

		return newToken;
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
			requestedAttributes.addAll(si.scopeDefinition().attributes);

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

		ResponseType responseType = null;
		if (token.getResponseType() != null && !token.getResponseType().isEmpty())
		{
			responseType = ResponseType.parse(token.getResponseType());
			if (responseType.contains(OIDCResponseTypeValue.ID_TOKEN) && responseType.size() == 1)
				newClaims.putAll(userInfoClaimSet);
		}

		return config.getTokenSigner().sign(newClaims).serialize();
	}

	@Component
	public static class TokenServiceFactory
	{
		private final OAuthRequestValidatorFactory requestValidatorFactory;
		private final IdPEngine idPEngine;
		private final ClientAttributesProviderFactory clientAttributesProviderFactory;

		@Autowired
		public TokenServiceFactory(OAuthRequestValidatorFactory requestValidatorFactory,
				@Qualifier("insecure") IdPEngine idPEngine,
				ClientAttributesProviderFactory clientAttributesProviderFactory)
		{
			this.requestValidatorFactory = requestValidatorFactory;
			this.idPEngine = idPEngine;
			this.clientAttributesProviderFactory = clientAttributesProviderFactory;
		}

		public TokenService getTokenService(OAuthASProperties config)
		{
			return new TokenService(requestValidatorFactory.getOAuthRequestValidator(config), config,
					new OAuthIdPEngine(idPEngine), clientAttributesProviderFactory.getClientAttributeProvider(config));

		}
	}

}
