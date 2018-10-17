/**
 * Copyright (c) 2017 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.utils.json;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;

import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.exceptions.EngineException;

/**
 * Default formatter. 
 * @author P.Piernik
 *
 */
@Component
public class DefaultJsonFormatterFacility implements TokenContentsJsonSerializer
{
	public static final String NAME = "base64";

	@Override
	public String getDescription()
	{
		return "Default formatter";
	}

	@Override
	public String getName()
	{
		return NAME;
	}

	@Override
	public JsonNode toJson(byte[] rawValue) throws EngineException
	{
		return  Constants.MAPPER.convertValue(rawValue, JsonNode.class);		
	}

}
