/**
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as.token.utils;

import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.utils.json.JsonBytesFacility;
import pl.edu.icm.unity.oauth.as.OAuthProcessor;

/**
 * Map refresh token contents to JsonNode
 * @author P.Piernik
 *
 */
@Component
public class JsonRefreshTokenFacility extends JsonBytesFacility
{

	@Override
	public String getDescription()
	{
		return "Refresh token JSON formatter";
	}

	@Override
	public String getName()
	{
		return OAuthProcessor.INTERNAL_REFRESH_TOKEN;
	}

}
