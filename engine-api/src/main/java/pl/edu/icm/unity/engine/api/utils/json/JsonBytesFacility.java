/**
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.utils.json;

import java.io.IOException;

import com.fasterxml.jackson.databind.JsonNode;

import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.exceptions.EngineException;

/**
 * Base for all formatter facilities which decode byte[] to json
 * 
 * @author P.Piernik
 *
 */
public class JsonBytesFacility implements JsonFormatterFacility
{
	public static final String NAME = "Bytes";
	
	@Override
	public String getDescription()
	{
		return "Bytes formatter";
	}

	@Override
	public String getName()
	{
		return NAME;
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
