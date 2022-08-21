/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.oauth.as.token;

import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.exceptions.EngineException;

@Component
public class ClientTokensCleaner
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_OAUTH, OAuthRefreshTokenExpirationListener.class);

	private final OAuthAccessTokenRepository accessTokenRepository;
	private final OAuthRefreshTokenRepository refreshTokenRepository;

	public ClientTokensCleaner(OAuthAccessTokenRepository accessTokenRepository,
			OAuthRefreshTokenRepository refreshTokenRepository)
	{
		this.accessTokenRepository = accessTokenRepository;
		this.refreshTokenRepository = refreshTokenRepository;
	}

	public void removeTokensForClient(long clientId, long userId, String rollingTokenId)
	{

		try
		{
			accessTokenRepository.removeOwnedByClient(clientId, userId);
		} catch (EngineException e)
		{
			log.error("Can not remove access tokens for client {}", clientId);
		}

		try
		{
			refreshTokenRepository.clearHistoryForClient(rollingTokenId, clientId, userId);
		} catch (EngineException e)
		{
			log.error("Can not remove refresh token history for client {}", clientId);

		}
	}
}
