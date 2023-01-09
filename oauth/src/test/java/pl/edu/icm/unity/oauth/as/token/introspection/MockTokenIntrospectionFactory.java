/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.oauth.as.token.introspection;

import static org.mockito.Mockito.mock;

import pl.edu.icm.unity.engine.api.token.SecuredTokensManagement;
import pl.edu.icm.unity.engine.api.token.TokensManagement;
import pl.edu.icm.unity.oauth.as.OAuthTestUtils;
import pl.edu.icm.unity.oauth.as.token.access.OAuthAccessTokenRepository;
import pl.edu.icm.unity.oauth.as.token.access.OAuthRefreshTokenRepository;

public class MockTokenIntrospectionFactory
{
	public static TokenIntrospectionResource createIntrospectionResource(TokensManagement tokensManagement)
	{		
		RemoteTokenIntrospectionService remoteTokenIntrospectionService = mock(RemoteTokenIntrospectionService.class);
		
		OAuthRefreshTokenRepository refreshTokenRepository = new OAuthRefreshTokenRepository(tokensManagement,
				mock(SecuredTokensManagement.class));
		OAuthAccessTokenRepository accessTokenRepository = new OAuthAccessTokenRepository(tokensManagement,
				mock(SecuredTokensManagement.class));

		return new TokenIntrospectionResource(remoteTokenIntrospectionService,
				new LocalTokenIntrospectionService(accessTokenRepository, refreshTokenRepository),
				OAuthTestUtils.ISSUER);
	}
}
