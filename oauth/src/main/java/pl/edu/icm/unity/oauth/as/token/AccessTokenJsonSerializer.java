/*
 * Copyright (c) 2017 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as.token;

import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.utils.json.TokenWithJsonContentsSerializer;

/**
 * Map access token contents to JsonNode
 * @author P.Piernik
 *
 */
@Component
class AccessTokenJsonSerializer extends TokenWithJsonContentsSerializer
{
	AccessTokenJsonSerializer()
	{
		super(OAuthAccessTokenRepository.INTERNAL_ACCESS_TOKEN, "Access token JSON formatter");
	}
}
