/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as.token.access;

import static org.assertj.core.api.Assertions.assertThat;
import static pl.edu.icm.unity.oauth.as.token.access.AccessTokenFactory.JWT_AT_MEDIA_TYPE;

import java.security.interfaces.ECPublicKey;
import java.text.ParseException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.oauth2.sdk.token.AccessToken;
import com.nimbusds.openid.connect.sdk.claims.UserInfo;

import pl.edu.icm.unity.oauth.as.OAuthASProperties.AccessTokenFormat;
import pl.edu.icm.unity.oauth.as.token.OAuthErrorException;
import pl.edu.icm.unity.oauth.as.webauthz.ClaimsInTokenAttribute;
import pl.edu.icm.unity.oauth.as.MockPKIMan;
import pl.edu.icm.unity.oauth.as.OAuthASProperties;
import pl.edu.icm.unity.oauth.as.OAuthTestUtils;
import pl.edu.icm.unity.oauth.as.OAuthToken;
import pl.edu.icm.unity.oauth.as.TokenSigner;

public class AccessTokenFactoryTest
{
	private static final MockPKIMan PKI_MANAMGENET = new MockPKIMan(
				"src/test/resources/demoECKey.p12", "123456");

	@Test
	public void shouldCreateJWTTokenRequestedWithAccept() throws OAuthErrorException
	{
		AccessTokenFactory factory = getFactory(AccessTokenFormat.AS_REQUESTED);
		
		AccessToken accessToken = factory.create(getFakeToken(), new Date(1000), JWT_AT_MEDIA_TYPE);
		
		assertThat(isJWTToken(accessToken)).isTrue();
	}
	
	@Test
	public void shouldCreateJWTTokenWhenConfigured() throws OAuthErrorException
	{
		AccessTokenFactory factory = getFactory(AccessTokenFormat.JWT);
		
		AccessToken accessToken = factory.create(getFakeToken(), new Date(1000));
		
		assertThat(isJWTToken(accessToken)).isTrue();
	}
	
	@Test
	public void shouldCreateJWTTokenWithUserInfoAttrWhenClaimsInTokenIsUsed() throws OAuthErrorException, ParseException
	{
		AccessTokenFactory factory = getFactory(AccessTokenFormat.JWT);
		OAuthToken fakeToken = getFakeToken();
		fakeToken.setUserInfo(new UserInfo(JWTClaimsSet.parse(Map.of("attr1", "v1", "sub", "sub"))).toJSONObject()
				.toJSONString());
		fakeToken.setClaimsInTokenAttribute(Optional.of(ClaimsInTokenAttribute.builder()
				.withValues(Set.of(ClaimsInTokenAttribute.Value.token))
				.build()));

		AccessToken accessToken = factory.create(fakeToken, new Date(1000));
		SignedJWT parse = SignedJWT.parse(accessToken.getValue());
		assertThat(parse.getJWTClaimsSet()
				.getClaims()
				.get("attr1")).isEqualTo("v1");
	}
	
	@Test
	public void shouldCreateJWTTokenWithoutUserInfoAttrWhenClaimsInTokenIsNotUsed()
			throws OAuthErrorException, ParseException
	{
		AccessTokenFactory factory = getFactory(AccessTokenFormat.JWT);
		OAuthToken fakeToken = getFakeToken();
		fakeToken.setUserInfo(new UserInfo(JWTClaimsSet.parse(Map.of("attr1", "v1", "sub", "sub"))).toJSONObject()
				.toJSONString());
		fakeToken.setClaimsInTokenAttribute(Optional.empty());

		AccessToken accessToken = factory.create(fakeToken, new Date(1000));
		SignedJWT parse = SignedJWT.parse(accessToken.getValue());
		assertThat(parse.getJWTClaimsSet()
				.getClaims()
				.get("attr1")).isNull();
	}

	@Test
	public void shouldCreatePlainTokenWhenConfigured() throws OAuthErrorException
	{
		AccessTokenFactory factory = getFactory(AccessTokenFormat.PLAIN);
		
		AccessToken accessToken = factory.create(getFakeToken(), new Date(1000), JWT_AT_MEDIA_TYPE);
		
		assertThat(isPlainToken(accessToken)).isTrue();		
	}

	@Test
	public void shouldCreatePlainTokenWhenJWTNotRequested() throws OAuthErrorException
	{
		AccessTokenFactory factory = getFactory(AccessTokenFormat.AS_REQUESTED);
		
		AccessToken accessToken = factory.create(getFakeToken(), new Date(1000));
		
		assertThat(isPlainToken(accessToken)).isTrue();		
	}

	@Test
	public void shouldAddRequiredElementsToJWT() throws Exception
	{
		AccessTokenFactory factory = getFactory(AccessTokenFormat.JWT);
		
		AccessToken accessToken = factory.create(getFakeToken(), new Date(1000));
		
		assertThat(isJWTToken(accessToken)).isTrue();
		SignedJWT jwt = SignedJWT.parse(accessToken.getValue());
		assertThat(jwt.getHeader().getType().getType()).isEqualTo("at+jwt");
		JWTClaimsSet claimsSet = jwt.getJWTClaimsSet();
		assertThat(claimsSet.getIssuer()).isEqualTo("issuer");
		assertThat(claimsSet.getExpirationTime()).isEqualTo(new Date(1000));
		assertThat(claimsSet.getSubject()).isEqualTo("subject");
		assertThat(claimsSet.getClaims().get("client_id")).isEqualTo("client");
		assertThat(claimsSet.getIssueTime()).isNotNull();
		assertThat(claimsSet.getClaims().get("scope")).isEqualTo("sc1");
		assertThat(claimsSet.getAudience()).containsExactly("audience");
	}

	@Test
	public void shouldVerifySignatureOfJWT() throws Exception
	{
		AccessTokenFactory factory = getFactory(AccessTokenFormat.JWT);
		
		AccessToken accessToken = factory.create(getFakeToken(), new Date(1000));
		
		assertThat(isJWTToken(accessToken)).isTrue();
		SignedJWT jwt = SignedJWT.parse(accessToken.getValue());
		ECDSAVerifier verifier = new ECDSAVerifier((ECPublicKey) 
				PKI_MANAMGENET.getCredential("foo").getCertificate().getPublicKey());
		jwt.verify(verifier);
	}

	
	private boolean isJWTToken(AccessToken accessToken)
	{
		try
		{
			SignedJWT.parse(accessToken.getValue());
			return true;
		} catch (ParseException e)
		{
			return false;
		}
	}
	
	private boolean isPlainToken(AccessToken accessToken)
	{
		return !isJWTToken(accessToken);
	}
	
	public static AccessTokenFactory getFactory(AccessTokenFormat format)
	{
		OAuthASProperties config = OAuthTestUtils.getOIDCConfig();
		config.setProperty(OAuthASProperties.SIGNING_ALGORITHM,
				OAuthASProperties.SigningAlgorithms.ES512.toString());
		
		TokenSigner signer = new TokenSigner(config, PKI_MANAMGENET);
		return new AccessTokenFactory(format, signer);
	}
	
	public static OAuthToken getFakeToken()
	{
		OAuthToken ret = new OAuthToken();
		ret.setSubject("subject");
		ret.setIssuerUri("issuer");
		ret.setClientUsername("client");
		ret.setEffectiveScope(new String []{"sc1"});
		ret.setAudience(List.of("audience"));
		return ret;
	}
}
