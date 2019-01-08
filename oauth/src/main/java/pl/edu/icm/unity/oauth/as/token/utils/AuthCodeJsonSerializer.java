/**
 * Copyright (c) 2017 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as.token.utils;

import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.utils.json.TokenWithJsonContentsSerializer;
import pl.edu.icm.unity.oauth.as.OAuthProcessor;

/**
 * Map auth code token contents to JsonNode
 * @author P.Piernik
 *
 */
@Component
public class AuthCodeJsonSerializer extends TokenWithJsonContentsSerializer 
{
	public AuthCodeJsonSerializer()
	{
		super(OAuthProcessor.INTERNAL_CODE_TOKEN, "Auth code token JSON formatter");
	}
}
