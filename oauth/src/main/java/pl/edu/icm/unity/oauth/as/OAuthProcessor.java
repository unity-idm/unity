/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as;

import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import com.google.gwt.thirdparty.guava.common.collect.Lists;
import com.nimbusds.oauth2.sdk.AuthorizationCode;
import com.nimbusds.oauth2.sdk.AuthorizationSuccessResponse;
import com.nimbusds.oauth2.sdk.ResponseType;
import com.nimbusds.oauth2.sdk.id.Audience;
import com.nimbusds.oauth2.sdk.id.Issuer;
import com.nimbusds.oauth2.sdk.id.Subject;
import com.nimbusds.oauth2.sdk.token.AccessToken;
import com.nimbusds.oauth2.sdk.token.BearerAccessToken;
import com.nimbusds.openid.connect.sdk.OIDCResponseTypeValue;
import com.nimbusds.openid.connect.sdk.claims.ClaimsSet;
import com.nimbusds.openid.connect.sdk.claims.IDTokenClaimsSet;
import com.nimbusds.openid.connect.sdk.claims.UserInfo;

import pl.edu.icm.unity.oauth.as.OAuthSystemAttributesProvider.GrantFlow;
import pl.edu.icm.unity.oauth.as.webauthz.OAuthAuthzContext;
import pl.edu.icm.unity.server.api.internal.TokensManagement;
import pl.edu.icm.unity.server.translation.out.TranslationResult;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.IdentityParam;

/**
 * Groups OAuth related logic for processing the request and preparing the response.  
 * @author K. Benedyczak
 */
public class OAuthProcessor
{
	/**
	 * Returns only requested attributes for which we have mapping.
	 * @param userInfo
	 * @param ctx
	 * @return
	 */
	public Set<Attribute<?>> filterAttributes(TranslationResult userInfo, 
			OAuthAuthzContext ctx)
	{
		Set<Attribute<?>> ret = filterNotRequestedAttributes(userInfo, ctx);
		return filterUnupportedAttributes(ret);
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
	 */
	public AuthorizationSuccessResponse prepareAuthzResponseAndRecordInternalState(Collection<Attribute<?>> attributes, 
			IdentityParam identity,	OAuthAuthzContext ctx, TokensManagement tokensMan)
	{
		if (ctx.isOpenIdMode())
		{
			UserInfo userInfo = prepareUserInfoClaimSet(identity.getValue(), attributes);
			IDTokenClaimsSet idToken = prepareIdInfoClaimSet(identity.getValue(), ctx, userInfo);
			//TODO create JSON metadata for the internal token.
		}
		
		ResponseType responseType = ctx.getRequest().getResponseType(); 
		if (!(responseType.contains(OIDCResponseTypeValue.ID_TOKEN) && responseType.size() == 1))
		{
			//TODO create and record internal token
		}
		tokensMan.addToken(type, value, owner, contents, created, expires);
		
		AuthorizationSuccessResponse oauthResponse = null;
		if (GrantFlow.authorizationCode == ctx.getFlow())
		{
			AuthorizationCode authzCode = new AuthorizationCode();
			oauthResponse = new AuthorizationSuccessResponse(ctx.getReturnURI(), authzCode, 
					ctx.getRequest().getState());
		} else if (GrantFlow.implicit == ctx.getFlow())
		{
			AccessToken accessToken = new BearerAccessToken();
			oauthResponse = new AuthorizationSuccessResponse(ctx.getReturnURI(), accessToken, 
					ctx.getRequest().getState());
			//TODO - id_token request
		} else if (GrantFlow.openidHybrid == ctx.getFlow())
		{
			//TODO
		}
	}
	
	
	
	/**
	 * Returns a collection of attributes including only those attributes for which there is an OAuth 
	 * representation.
	 */
	private Set<Attribute<?>> filterUnupportedAttributes(Set<Attribute<?>> src)
	{
		Set<Attribute<?>> ret = new HashSet<Attribute<?>>();
		OAuthAttributeMapper mapper = new DefaultOAuthAttributeMapper();
		
		for (Attribute<?> a: src)
			if (mapper.isHandled(a))
				ret.add(a);
		return ret;
	}
	
	
	private Set<Attribute<?>> filterNotRequestedAttributes(TranslationResult translationResult, 
			OAuthAuthzContext ctx)
	{
		Collection<Attribute<?>> allAttrs = translationResult.getAttributes();
		Set<Attribute<?>> filteredAttrs = new HashSet<Attribute<?>>();
		
		for (Attribute<?> attr: allAttrs)
			if (ctx.getRequestedAttrs().contains(attr.getName()))
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
			ClaimsSet regularAttributes)
	{
		IDTokenClaimsSet idToken = new IDTokenClaimsSet(
				new Issuer(context.getIssuerName()), 
				new Subject(userIdentity), 
				Lists.newArrayList(new Audience(context.getClientName())), 
				new Date(System.currentTimeMillis() + context.getIdTokenValidity()*1000), 
				new Date());
		ResponseType responseType = context.getRequest().getResponseType(); 
		if (responseType.contains(OIDCResponseTypeValue.ID_TOKEN) && responseType.size() == 1)
			idToken.putAll(regularAttributes);
		return idToken;
	}
	
	private UserInfo prepareUserInfoClaimSet(String userIdentity, Collection<Attribute<?>> attributes)
	{
		UserInfo userInfo = new UserInfo(new Subject(userIdentity));
		
		OAuthAttributeMapper mapper = new DefaultOAuthAttributeMapper();
		
		for (Attribute<?> attr: attributes)
		{
			if (mapper.isHandled(attr))
			{
				String name = mapper.getJsonKey(attr);
				Object value = mapper.getJsonValue(attr);
				userInfo.setClaim(name, value);
			}
		}
		
		return userInfo;
	}
}
