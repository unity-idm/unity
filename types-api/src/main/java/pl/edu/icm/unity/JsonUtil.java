/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity;

import pl.edu.icm.unity.exceptions.InternalException;

import java.util.List;

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

	public static ObjectNode parse(byte[] contents)
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

	public static byte[] serialize2Bytes(JsonNode node)
	{
		try
		{
			return Constants.MAPPER.writeValueAsBytes(node);
		} catch (JsonProcessingException e)
		{
			throw new InternalException("Can't perform JSON serialization", e);
		}
	}
	
	public static <T> T parse(String contents, Class<T> clazz)
	{
		try
		{
			return Constants.MAPPER.readValue(contents, clazz);
		} catch (Exception e)
		{
			throw new InternalException("Can't perform JSON deserialization", e);
		}
	}

	public static <T> List<T> parseToList(String contents, Class<T> clazz)
	{
		try
		{
			return Constants.MAPPER.readValue(contents,
					Constants.MAPPER.getTypeFactory().constructCollectionType(List.class, clazz));
		} catch (Exception e)
		{
			throw new InternalException("Can't perform JSON deserialization", e);
		}
	}

	public static String toJsonString(Object value)
	{
		try
		{
			return Constants.MAPPER.writeValueAsString(value);
		} catch (JsonProcessingException e)
		{
			throw new InternalException("Can't perform JSON serialization", e);
		}
	}
}
