/**
 * Copyright (c) 2017 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as.token.access;

import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.utils.json.TokenWithJsonContentsSerializer;

/**
 * Map refresh token contents to JsonNode
 * @author P.Piernik
 */
@Component
public class RefreshTokenJsonSerializer extends TokenWithJsonContentsSerializer
{
	public RefreshTokenJsonSerializer()
	{
		super(OAuthRefreshTokenRepository.INTERNAL_REFRESH_TOKEN, "Refresh token JSON formatter");
	}
}
