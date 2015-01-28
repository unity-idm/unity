/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.utils;

import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.exceptions.InternalException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Simplifies JSON parsing a little bit
 * @author K. Benedyczak
 */
public class JsonUtil
{
	public static ObjectNode parse(String contents)
	{
		try
		{
			return Constants.MAPPER.readValue(contents, ObjectNode.class);
		} catch (Exception e)
		{
			throw new InternalException("Can't perform JSON deserialization", e);
		}
	}
	
	public static String serialize(JsonNode node)
	{
		try
		{
			return Constants.MAPPER.writeValueAsString(node);
		} catch (JsonProcessingException e)
		{
			throw new InternalException("Can't perform JSON serialization", e);
		}
	}
}
