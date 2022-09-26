/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.oauth.as.token;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.token.Token;
import pl.edu.icm.unity.engine.api.token.TokensManagement;
import pl.edu.icm.unity.engine.api.token.TokensManagement.TokenExpirationListener;
import pl.edu.icm.unity.oauth.as.OAuthToken;
import pl.edu.icm.unity.oauth.as.token.access.OAuthRefreshTokenRepository;

class OAuthRefreshTokenExpirationListener implements TokenExpirationListener
{
	private final OAuthClientTokensCleaner tokenCleaner;

	OAuthRefreshTokenExpirationListener(OAuthClientTokensCleaner tokenCleaner)
	{
		this.tokenCleaner = tokenCleaner;
	}

	@Override
	public void tokenExpired(Token token)
	{
		OAuthToken refreshToken = OAuthToken.getInstanceFromJson(token.getContents());
		tokenCleaner.removeTokensForClient(refreshToken.getClientId(), token.getOwner(), refreshToken.getFirstRefreshRollingToken());
	}

	@Component
	class OAuthRefreshTokenExpirationListenerInstalator
	{
		@Autowired
		public OAuthRefreshTokenExpirationListenerInstalator(TokensManagement tokensManagement,
				OAuthClientTokensCleaner tokenCleaner)
		{
			tokensManagement.addTokenExpirationListener(new OAuthRefreshTokenExpirationListener(tokenCleaner),
					OAuthRefreshTokenRepository.INTERNAL_REFRESH_TOKEN);
		}
	}
}