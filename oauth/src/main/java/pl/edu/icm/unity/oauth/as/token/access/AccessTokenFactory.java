/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as.token.access;

import java.util.Date;
import java.util.UUID;

import javax.ws.rs.core.Response;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.JWTClaimsSet.Builder;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.oauth2.sdk.OAuth2Error;
import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.Scope;
import com.nimbusds.oauth2.sdk.token.AccessToken;
import com.nimbusds.oauth2.sdk.token.BearerAccessToken;
import com.nimbusds.openid.connect.sdk.claims.UserInfo;

import pl.edu.icm.unity.oauth.as.OAuthASProperties;
import pl.edu.icm.unity.oauth.as.OAuthASProperties.AccessTokenFormat;
import pl.edu.icm.unity.oauth.as.token.BaseTokenResource;
import pl.edu.icm.unity.oauth.as.token.BearerJWTAccessToken;
import pl.edu.icm.unity.oauth.as.token.OAuthErrorException;
import pl.edu.icm.unity.oauth.as.OAuthToken;
import pl.edu.icm.unity.oauth.as.TokenSigner;

public class AccessTokenFactory
{
	static final String JWT_AT_MEDIA_TYPE = "application/at+jwt";
	private final AccessTokenFormat configuredFormat;
	private final TokenSigner tokenSigner;

	public AccessTokenFactory(OAuthASProperties configuration)
	{
		this(configuration.getAccessTokenFormat(), configuration.getTokenSigner());
	}

	public AccessTokenFactory(AccessTokenFormat configuredFormat, TokenSigner tokenSigner)
	{
		this.configuredFormat = configuredFormat;
		this.tokenSigner = tokenSigner;
	}


	public AccessToken create(OAuthToken token, Date issueTime) throws OAuthErrorException
	{
		return configuredFormat == AccessTokenFormat.JWT ? 
				createJWTAccessToken(token, issueTime) : createPlainAccessToken(token);
	}

	public AccessToken create(OAuthToken token, Date issueTime, String acceptedMimeHeader) throws OAuthErrorException
	{
		if (configuredFormat == AccessTokenFormat.AS_REQUESTED && jwtRequested(acceptedMimeHeader))
			return createJWTAccessToken(token, issueTime);
		return create(token, issueTime);
	}

	private boolean jwtRequested(String acceptedMimeHeader)
	{
		return JWT_AT_MEDIA_TYPE.equals(acceptedMimeHeader);
	}

	private AccessToken createPlainAccessToken(OAuthToken token)
	{
		int tokenValidity = token.getTokenValidity();
		return new BearerAccessToken(tokenValidity, new Scope(token.getEffectiveScope()));
	}
	
	private AccessToken createJWTAccessToken(OAuthToken token, Date issueTime) throws OAuthErrorException
	{
		Scope scope = new Scope(token.getEffectiveScope());
		Builder claimsSetBuilder = new JWTClaimsSet.Builder()
				.subject(token.getSubject())
				.issueTime(issueTime)
				.issuer(token.getIssuerUri())
				.audience(token.getAudience())
				.expirationTime(new Date(issueTime.getTime() + token.getTokenValidity()*1000))
				.jwtID(UUID.randomUUID().toString())
				.claim("scope", scope.toString())
				.claim("client_id", token.getClientUsername());

		addAttributesToClaimIfNeeded(claimsSetBuilder, token);
		JWTClaimsSet claimsSet = claimsSetBuilder.build();
		SignedJWT signedJWT = sign(claimsSet);
		int tokenValidity = token.getTokenValidity();
		return new BearerJWTAccessToken(signedJWT.serialize(), tokenValidity, scope, claimsSet);
	}
	
	private void addAttributesToClaimIfNeeded(Builder claimsSetBuilder, OAuthToken token) throws OAuthErrorException
	{
		if (token.hasSupportAttributesInToken())
		{
			UserInfo userInfo;
			try
			{
				userInfo = UserInfo.parse(token.getUserInfo());
				userInfo.toJWTClaimsSet().getClaims().forEach(claimsSetBuilder::claim);
			} catch (ParseException e)
			{
				Response errorResponse = BaseTokenResource.makeError(OAuth2Error.INVALID_REQUEST, 
						"Invalid user info claim set");
				throw new OAuthErrorException(errorResponse);
			}
			
		}
	}
	
	private SignedJWT sign(JWTClaimsSet claimsSet) throws OAuthErrorException
	{
		try
		{
			return tokenSigner.sign(claimsSet, "at+jwt");
		} catch (JOSEException e)
		{
			Response errorResponse = BaseTokenResource.makeError(OAuth2Error.SERVER_ERROR, 
					"server configuration problem");
			throw new OAuthErrorException(errorResponse);
		}
	}
}
