/**
 * Copyright (c) 2017 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.utils.json;

import java.io.IOException;

import com.fasterxml.jackson.databind.JsonNode;

import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.exceptions.EngineException;

/**
 * Base for all tokens where contents is storing byte[] serialized JSON. I.e. nearly 
 * a copy serializer.
 * 
 * @author P.Piernik
 *
 */
public abstract class TokenWithJsonContentsSerializer implements TokenContentsJsonSerializer
{
	private String supportedType;
	private String description;
	
	public TokenWithJsonContentsSerializer(String supportedType, String description)
	{
		this.supportedType = supportedType;
		this.description = description;
	}

	@Override
	public String getDescription()
	{
		return description;
	}

	@Override
	public String getName()
	{
		return supportedType;
	}
	
	public JsonNode toJson(byte[] rawValue) throws EngineException
	{
		JsonNode node = null;
		try
		{
			node = Constants.MAPPER.readTree(rawValue);
		} catch (IOException e)
		{
			throw new EngineException("Cannot parse to json", e);
		}
		return node;
	}
}
