/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.oauth.as.token;

import java.util.ArrayList;
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
import com.nimbusds.jwt.SignedJWT;
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

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.idp.EntityInGroup;
import pl.edu.icm.unity.engine.api.idp.IdPEngine;
import pl.edu.icm.unity.engine.api.translation.ExecutionFailException;
import pl.edu.icm.unity.engine.api.translation.out.TranslationResult;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IllegalGroupValueException;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.oauth.as.OAuthASProperties;
import pl.edu.icm.unity.oauth.as.OAuthProcessor;
import pl.edu.icm.unity.oauth.as.OAuthRequestValidator;
import pl.edu.icm.unity.oauth.as.OAuthScope;
import pl.edu.icm.unity.oauth.as.OAuthSystemAttributesProvider;
import pl.edu.icm.unity.oauth.as.OAuthToken;
import pl.edu.icm.unity.oauth.as.OAuthRequestValidator.OAuthRequestValidatorFactory;
import pl.edu.icm.unity.oauth.as.webauthz.OAuthIdPEngine;
import pl.edu.icm.unity.types.basic.AttributeExt;
import pl.edu.icm.unity.types.basic.DynamicAttribute;
import pl.edu.icm.unity.types.basic.EntityParam;

public class TokenUtils
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_OAUTH, TokenUtils.class);

	private final OAuthRequestValidator requestValidator;
	private final OAuthASProperties config;
	private final OAuthIdPEngine notAuthorizedOauthIdpEngine;

	public TokenUtils(OAuthRequestValidator requestValidator, OAuthASProperties config,
			OAuthIdPEngine notAuthorizedOauthIdpEngine)
	{
		this.requestValidator = requestValidator;
		this.config = config;
		this.notAuthorizedOauthIdpEngine = notAuthorizedOauthIdpEngine;
	}

	OAuthToken prepareNewToken(OAuthToken oldToken, String scope, List<String> oldRequestedScopesList, long ownerId,
			long clientId, String clientUserName, boolean createIdToken, String grant) throws OAuthErrorException
	{
		OAuthToken newToken = new OAuthToken(oldToken);

		List<String> newRequestedScopeList = new ArrayList<>();
		if (scope != null && !scope.isEmpty())
		{
			newRequestedScopeList.addAll(Arrays.asList(scope.split(" ")));
		}

		if (!oldRequestedScopesList.containsAll(newRequestedScopeList))
		{
			throw new OAuthErrorException(BaseOAuthResource.makeError(OAuth2Error.INVALID_SCOPE, "wrong scope"));
		}
		newToken.setRequestedScope(newRequestedScopeList.stream().toArray(String[]::new));

		// get new attributes for identity
		TranslationResult userInfoRes = getAttributes(clientId, ownerId, grant);

		List<OAuthScope> newValidRequestedScopes = requestValidator.getValidRequestedScopes(
				getClientAttributes(new EntityParam(clientId)), Scope.parse(String.join(" ", newRequestedScopeList)));
		newToken.setEffectiveScope(newValidRequestedScopes.stream().map(s -> s.name).toArray(String[]::new));

		UserInfo userInfoClaimSet = createUserInfo(newValidRequestedScopes, newToken.getSubject(), userInfoRes);
		newToken.setUserInfo(userInfoClaimSet.toJSONObject().toJSONString());

		Date now = new Date();
		// if openid mode build new id_token using new userinfo.
		if (newRequestedScopeList.contains(OIDCScopeValue.OPENID.getValue()) && createIdToken)
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

	private Map<String, AttributeExt> getClientAttributes(EntityParam entity) throws OAuthErrorException
	{
		try
		{
			return requestValidator.getAttributesNoAuthZ(entity);
		} catch (Exception e)
		{
			throw new OAuthErrorException(BaseOAuthResource.makeError(OAuth2Error.SERVER_ERROR, e.getMessage()));
		}
	}

	private UserInfo createUserInfo(List<OAuthScope> validScopes, String userIdentity, TranslationResult userInfoRes)
	{
		Set<String> requestedAttributes = new HashSet<>();
		for (OAuthScope si : validScopes)
			requestedAttributes.addAll(si.attributes);

		Collection<DynamicAttribute> attributes = OAuthProcessor.filterAttributes(userInfoRes, requestedAttributes);
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
				new Subject(token.getSubject()), audience, getAccessTokenExpiration(config, now), now);
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
	
	String getClientName(EntityParam entity) throws OAuthErrorException
	{
		Map<String, AttributeExt> attributes = getClientAttributes(entity);
		AttributeExt nameA = attributes.get(OAuthSystemAttributesProvider.CLIENT_NAME);
		if (nameA != null)
			return ((String) nameA.getValues().get(0));
		else
			return null;
	}

	
	Date getAccessTokenExpiration(OAuthASProperties config, Date now)
	{
		int accessTokenValidity = config.getAccessTokenValidity();
		
		return new Date(now.getTime() + accessTokenValidity * 1000);
	}
	
	AccessTokenResponse getAccessTokenResponse(OAuthToken internalToken,
			AccessToken accessToken, RefreshToken refreshToken,
			Map<String, Object> additionalParams)
	{
		JWT signedJWT = decodeIDToken(internalToken);
		AccessTokenResponse oauthResponse = signedJWT == null
				? new AccessTokenResponse(new Tokens(accessToken, refreshToken),
						additionalParams)
				: new OIDCTokenResponse(new OIDCTokens(signedJWT, accessToken,
						refreshToken), additionalParams);
		return oauthResponse;
	}
	
	private JWT decodeIDToken(OAuthToken internalToken)
	{
		try
		{
			return internalToken.getOpenidInfo() == null ? null : 
				SignedJWT.parse(internalToken.getOpenidInfo());
		} catch (java.text.ParseException e)
		{
			throw new InternalException("Can not parse the internal id token", e);
		}
	}
	
	
	@Component
	public static class TokenUtilsFactory
	{
		private final OAuthRequestValidatorFactory requestValidatorFactory;
		private final IdPEngine idPEngine;
		
		@Autowired
		public TokenUtilsFactory(OAuthRequestValidatorFactory requestValidatorFactory, @Qualifier("insecure")  IdPEngine idPEngine)
		{
			this.requestValidatorFactory = requestValidatorFactory;
			this.idPEngine = idPEngine;
		}
		
		public TokenUtils getTokenUtils(OAuthASProperties config)
		{
			return new TokenUtils(requestValidatorFactory.getOAuthRequestValidator(config), config, new OAuthIdPEngine(idPEngine));
				
		}
		
	}
	
}
