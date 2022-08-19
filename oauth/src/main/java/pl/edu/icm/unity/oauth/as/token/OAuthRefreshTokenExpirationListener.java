/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.oauth.as.token;

import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.token.Token;
import pl.edu.icm.unity.engine.api.token.TokensManagement;
import pl.edu.icm.unity.engine.api.token.TokensManagement.TokenExpirationListener;
import pl.edu.icm.unity.oauth.as.OAuthToken;

class OAuthRefreshTokenExpirationListener implements TokenExpirationListener
{

	private final TokenCleaner tokenCleaner;

	public OAuthRefreshTokenExpirationListener(TokenCleaner tokenCleaner)
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
		public OAuthRefreshTokenExpirationListenerInstalator(TokensManagement tokensManagement,
				TokenCleaner tokenCleaner)
		{
			tokensManagement.addTokenExpirationListener(new OAuthRefreshTokenExpirationListener(tokenCleaner),
					OAuthRefreshTokenRepository.INTERNAL_REFRESH_TOKEN);
		}

	}
}