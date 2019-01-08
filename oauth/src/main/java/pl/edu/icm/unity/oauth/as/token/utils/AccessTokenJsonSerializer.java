/*
 * Copyright (c) 2017 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as.token.utils;

import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.utils.json.TokenWithJsonContentsSerializer;
import pl.edu.icm.unity.oauth.as.OAuthProcessor;

/**
 * Map access token contents to JsonNode
 * @author P.Piernik
 *
 */
@Component
public class AccessTokenJsonSerializer extends TokenWithJsonContentsSerializer
{
	public AccessTokenJsonSerializer()
	{
		super(OAuthProcessor.INTERNAL_ACCESS_TOKEN, "Access token JSON formatter");
	}
}
