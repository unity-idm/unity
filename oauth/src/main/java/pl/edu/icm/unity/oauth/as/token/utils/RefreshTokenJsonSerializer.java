/**
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as.token.utils;

import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.utils.json.TokenWithJsonContentsSerializer;
import pl.edu.icm.unity.oauth.as.OAuthProcessor;

/**
 * Map refresh token contents to JsonNode
 * @author P.Piernik
 */
@Component
public class RefreshTokenJsonSerializer extends TokenWithJsonContentsSerializer
{
	public RefreshTokenJsonSerializer()
	{
		super(OAuthProcessor.INTERNAL_REFRESH_TOKEN, "Refresh token JSON formatter");
	}
}
