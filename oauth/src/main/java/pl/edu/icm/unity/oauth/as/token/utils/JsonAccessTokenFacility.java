/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as.token.utils;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.base.token.Token;
import pl.edu.icm.unity.engine.api.utils.json.JsonFormatterFacility;
import pl.edu.icm.unity.oauth.as.OAuthProcessor;

/**
 * Map access token to json ObjectNode
 * @author P.Piernik
 *
 */
@Component
public class JsonAccessTokenFacility extends JsonOAuthTokenFacility implements JsonFormatterFacility
{

	@Override
	public String getDescription()
	{
		return "Access token JSON formatter";
	}

	@Override
	public String getName()
	{
		return OAuthProcessor.INTERNAL_ACCESS_TOKEN;
	}

	@Override
	public ObjectNode toJson(Object o)
	{
		return toJson((Token) o);
	}

}
