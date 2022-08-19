/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as;

import java.util.Date;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import com.nimbusds.oauth2.sdk.client.ClientType;
import com.nimbusds.oauth2.sdk.token.AccessToken;

import pl.edu.icm.unity.base.token.Token;
import pl.edu.icm.unity.engine.api.token.TokensManagement;
import pl.edu.icm.unity.oauth.as.OAuthASProperties.AccessTokenFormat;
import pl.edu.icm.unity.oauth.as.token.AccessTokenFactory;
import pl.edu.icm.unity.oauth.as.token.AccessTokenFactoryTest;
import pl.edu.icm.unity.oauth.as.token.BaseOAuthResource;
import pl.edu.icm.unity.oauth.as.token.OAuthAccessTokenRepository;
import pl.edu.icm.unity.types.basic.EntityParam;

public class OAuthTokenRepositoryTest
{
	@Test
	public void shouldReadStoredJWTToken() throws Exception
	{
		TokensManagement tokensMan = new MockTokensMan();
		OAuthAccessTokenRepository repository = new OAuthAccessTokenRepository(tokensMan, null);
		
		AccessTokenFactory factory = AccessTokenFactoryTest.getFactory(AccessTokenFormat.JWT);
		OAuthToken oauthToken = AccessTokenFactoryTest.getFakeToken();
		oauthToken.setClientType(ClientType.CONFIDENTIAL);
		AccessToken accessToken = factory.create(oauthToken, new Date(100));
		repository.storeAccessToken(accessToken, oauthToken, 
				new EntityParam(100l), new Date(100), new Date(Long.MAX_VALUE));
		
		Token loadedToken = repository.readAccessToken(accessToken.getValue());
		
		Assertions.assertThat(loadedToken).isNotNull();
		OAuthToken loadedOauthToken = BaseOAuthResource.parseInternalToken(loadedToken);
		Assertions.assertThat(loadedOauthToken).isEqualTo(oauthToken);
	}
}
