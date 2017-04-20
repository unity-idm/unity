/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as;

import java.security.PrivateKey;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.RSAPrivateKey;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.gwt.thirdparty.guava.common.collect.Lists;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.oauth2.sdk.AuthorizationCode;
import com.nimbusds.oauth2.sdk.AuthorizationSuccessResponse;
import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.ResponseType;
import com.nimbusds.oauth2.sdk.id.Audience;
import com.nimbusds.oauth2.sdk.id.Issuer;
import com.nimbusds.oauth2.sdk.id.Subject;
import com.nimbusds.oauth2.sdk.token.AccessToken;
import com.nimbusds.oauth2.sdk.token.BearerAccessToken;
import com.nimbusds.openid.connect.sdk.AuthenticationRequest;
import com.nimbusds.openid.connect.sdk.AuthenticationSuccessResponse;
import com.nimbusds.openid.connect.sdk.OIDCResponseTypeValue;
import com.nimbusds.openid.connect.sdk.claims.ClaimsSet;
import com.nimbusds.openid.connect.sdk.claims.IDTokenClaimsSet;
import com.nimbusds.openid.connect.sdk.claims.UserInfo;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IllegalIdentityValueException;
import pl.edu.icm.unity.exceptions.IllegalTypeException;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.oauth.as.OAuthSystemAttributesProvider.GrantFlow;
import pl.edu.icm.unity.server.api.internal.TokensManagement;
import pl.edu.icm.unity.server.translation.out.TranslationResult;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.DynamicAttribute;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.IdentityParam;

/**
 * Groups OAuth related logic for processing the request and preparing the response.  
 * @author K. Benedyczak
 */
public class OAuthProcessor
{
	public static final String INTERNAL_CODE_TOKEN = "oauth2Code";
	public static final String INTERNAL_ACCESS_TOKEN = "oauth2Access";
	
	/**
	 * Returns only requested attributes for which we have mapping.
	 * @param userInfo
	 * @param ctx
	 * @return
	 */
	public Set<DynamicAttribute> filterAttributes(TranslationResult userInfo, 
			Set<String> requestedAttributes)
	{
		Set<DynamicAttribute> ret = filterNotRequestedAttributes(userInfo, requestedAttributes);
		return filterUnsupportedAttributes(ret);
	}

	/**
	 * Returns Authorization response to be returned and records (if needed) 
	 * the internal state token, which is needed to associate further use of the code and/or id tokens with
	 * the authorization that currently takes place.
	 * @param attributes
	 * @param identity
	 * @param ctx
	 * @param tokensMan
	 * @return
	 * @throws JsonProcessingException 
	 * @throws JOSEException 
	 * @throws ParseException 
	 * @throws IllegalTypeException 
	 * @throws IllegalIdentityValueException 
	 * @throws WrongArgumentException 
	 */
	public AuthorizationSuccessResponse prepareAuthzResponseAndRecordInternalState(Collection<DynamicAttribute> attributes, 
			IdentityParam identity,	OAuthAuthzContext ctx, TokensManagement tokensMan) 
					throws EngineException, JsonProcessingException, ParseException, JOSEException
	{
		OAuthToken internalToken = new OAuthToken();
		internalToken.setScope(ctx.getEffectiveRequestedScopesList());
		internalToken.setClientId(ctx.getClientEntityId());
		internalToken.setRedirectUri(ctx.getReturnURI().toASCIIString());
		internalToken.setClientName(ctx.getClientName());
		internalToken.setClientUsername(ctx.getClientUsername());
		internalToken.setSubject(identity.getValue());
		internalToken.setMaxExtendedValidity(ctx.getConfig().getMaxExtendedAccessTokenValidity());
		internalToken.setTokenValidity(ctx.getConfig().getAccessTokenValidity());
		
		Date now = new Date();
		
		JWT idTokenSigned = null;
		ResponseType responseType = ctx.getRequest().getResponseType();
		
		UserInfo userInfo = prepareUserInfoClaimSet(identity.getValue(), attributes);
		internalToken.setUserInfo(userInfo.toJSONObject().toJSONString());

		if (ctx.isOpenIdMode())
		{
			IDTokenClaimsSet idToken = prepareIdInfoClaimSet(identity.getValue(), ctx, userInfo, now);
			idTokenSigned = signIdToken(idToken, ctx);
			internalToken.setOpenidToken(idTokenSigned.serialize());
			//we record OpenID token in internal state always in open id mode. However it may happen
			//that it is not requested immediately now:
			if (!responseType.contains(OIDCResponseTypeValue.ID_TOKEN))
				idTokenSigned = null;
		}
		
		AuthorizationSuccessResponse oauthResponse = null;
		if (GrantFlow.authorizationCode == ctx.getFlow())
		{
			AuthorizationCode authzCode = new AuthorizationCode();
			internalToken.setAuthzCode(authzCode.getValue());
			oauthResponse = new AuthorizationSuccessResponse(ctx.getReturnURI(), authzCode, null,
					ctx.getRequest().getState(), ctx.getRequest().impliedResponseMode());
			Date expiration = new Date(now.getTime() + ctx.getConfig().getCodeTokenValidity() * 1000);
			tokensMan.addToken(INTERNAL_CODE_TOKEN, authzCode.getValue(), 
					new EntityParam(identity), internalToken.getSerialized(), now, expiration);
		} else if (GrantFlow.implicit == ctx.getFlow())
		{
			if (responseType.contains(OIDCResponseTypeValue.ID_TOKEN) && responseType.size() == 1)
			{
				//we return only the id token, no access token so we don't need an internal token.
				return new AuthenticationSuccessResponse(
						ctx.getReturnURI(), null, idTokenSigned, 
						null, ctx.getRequest().getState(), null, 
						ctx.getRequest().impliedResponseMode());
			}

			AccessToken accessToken = new BearerAccessToken();
			internalToken.setAccessToken(accessToken.getValue());
			Date expiration = new Date(now.getTime() + ctx.getConfig().getAccessTokenValidity() * 1000);
			oauthResponse = new AuthenticationSuccessResponse(
						ctx.getReturnURI(), null, idTokenSigned, 
						accessToken, ctx.getRequest().getState(), null, 
						ctx.getRequest().impliedResponseMode());
			tokensMan.addToken(INTERNAL_ACCESS_TOKEN, accessToken.getValue(), 
					new EntityParam(identity), internalToken.getSerialized(), now, expiration);
		} else if (GrantFlow.openidHybrid == ctx.getFlow())
		{
			//in hybrid mode authz code is returned always
			AuthorizationCode authzCode = new AuthorizationCode();
			internalToken.setAuthzCode(authzCode.getValue());
			Date codeExpiration = new Date(now.getTime() + ctx.getConfig().getCodeTokenValidity() * 1000);
			tokensMan.addToken(INTERNAL_CODE_TOKEN, authzCode.getValue(), 
					new EntityParam(identity), internalToken.getSerialized(), 
					now, codeExpiration);
			
			//access token - sometimes
			AccessToken accessToken = null;
			if (responseType.contains(ResponseType.Value.TOKEN))
			{
				accessToken = new BearerAccessToken();
				internalToken.setAccessToken(accessToken.getValue());
				Date accessExpiration = new Date(now.getTime() + ctx.getConfig().getAccessTokenValidity() * 1000);
				tokensMan.addToken(INTERNAL_ACCESS_TOKEN, accessToken.getValue(), 
						new EntityParam(identity), internalToken.getSerialized(), 
						now, accessExpiration);
			}
			
			//id token also sometimes, but this was handled before.
			oauthResponse = new AuthenticationSuccessResponse(
					ctx.getReturnURI(), authzCode, idTokenSigned, 
					accessToken, ctx.getRequest().getState(), null, 
					ctx.getRequest().impliedResponseMode());
		}
		
		return oauthResponse;
	}
	
	
	
	/**
	 * Returns a collection of attributes including only those attributes for which there is an OAuth 
	 * representation.
	 */
	private Set<DynamicAttribute> filterUnsupportedAttributes(Set<DynamicAttribute> src)
	{
		Set<DynamicAttribute> ret = new HashSet<DynamicAttribute>();
		OAuthAttributeMapper mapper = new DefaultOAuthAttributeMapper();
		
		for (DynamicAttribute a: src)
			if (mapper.isHandled(a.getAttribute()))
				ret.add(a);
		return ret;
	}
	
	
	private Set<DynamicAttribute> filterNotRequestedAttributes(TranslationResult translationResult, 
			Set<String> requestedAttributes)
	{
		Collection<DynamicAttribute> allAttrs = translationResult.getAttributes();
		Set<DynamicAttribute> filteredAttrs = new HashSet<DynamicAttribute>();
		
		for (DynamicAttribute attr: allAttrs)
			if (requestedAttributes.contains(attr.getAttribute().getName()))
				filteredAttrs.add(attr);
		return filteredAttrs;
	}
	
	
	/**
	 * Creates an OIDC ID Token. The token includes regular attributes if and only if the access token is 
	 * not issued in the flow. This is the case if the only response type is 'id_token'. Section 5.4 of 
	 * OIDC specification.
	 *      
	 * @param userIdentity
	 * @param context
	 * @param regularAttributes
	 */
	private IDTokenClaimsSet prepareIdInfoClaimSet(String userIdentity, OAuthAuthzContext context, 
			ClaimsSet regularAttributes, Date now)
	{
		AuthenticationRequest request = (AuthenticationRequest) context.getRequest();
		String clientId = request.getClientID().getValue();
		IDTokenClaimsSet idToken = new IDTokenClaimsSet(
				new Issuer(context.getConfig().getIssuerName()), 
				new Subject(userIdentity), 
				Lists.newArrayList(new Audience(clientId)), 
				new Date(now.getTime() + context.getConfig().getIdTokenValidity()*1000), 
				now);
		ResponseType responseType = request.getResponseType(); 
		if (responseType.contains(OIDCResponseTypeValue.ID_TOKEN) && responseType.size() == 1)
			idToken.putAll(regularAttributes);
		if (request.getNonce() != null)
			idToken.setNonce(request.getNonce());
		return idToken;
	}
	
	public UserInfo prepareUserInfoClaimSet(String userIdentity, Collection<DynamicAttribute> attributes)
	{
		UserInfo userInfo = new UserInfo(new Subject(userIdentity));
		
		OAuthAttributeMapper mapper = new DefaultOAuthAttributeMapper();
		
		for (DynamicAttribute dat: attributes)
		{
			Attribute<?> attr = dat.getAttribute();
			if (mapper.isHandled(attr))
			{
				String name = mapper.getJsonKey(attr);
				Object value = mapper.getJsonValue(attr);
				userInfo.setClaim(name, value);
			}
		}
		
		return userInfo;
	}
	
	private JWT signIdToken(IDTokenClaimsSet idTokenClaims, OAuthAuthzContext ctx) 
			throws JOSEException, ParseException
	{
		PrivateKey pk = ctx.getConfig().getCredential().getKey();
		SignedJWT ret;
		JWSSigner signer;
		
		if (pk instanceof RSAPrivateKey)
		{
			ret = new SignedJWT(new JWSHeader(JWSAlgorithm.RS256), idTokenClaims.toJWTClaimsSet());
			signer = new RSASSASigner((RSAPrivateKey)pk);
		} else 
		{
			ret = new SignedJWT(new JWSHeader(JWSAlgorithm.ES256), 
					idTokenClaims.toJWTClaimsSet());
			signer = new ECDSASigner((ECPrivateKey)pk);
		}

		ret.sign(signer);
		return ret;
	}
}
