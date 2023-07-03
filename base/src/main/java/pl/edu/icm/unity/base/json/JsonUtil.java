/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.base.json;

import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.base.Constants;
import pl.edu.icm.unity.base.exceptions.InternalException;

/**
 * Simplifies JSON parsing a little bit
 * @author K. Benedyczak
 */
public class JsonUtil
{
	/**
	 * as {@link #getWithDef(JsonNode, String, String)} with the last argument (default) == null
	 * @param src
	 * @param name
	 * @return
	 */
	public static String getNullable(JsonNode src, String name)
	{
		return getWithDef(src, name, null);
	}
	
	/**
	 * Safely gets a string value from node. if undefined then given default is returned. If node is null,
	 * then null is defined.
	 * @param src
	 * @param name
	 * @param def
	 * @return
	 */
	public static String getWithDef(JsonNode src, String name, String def)
	{
		JsonNode n = src.get(name);
		return n != null ? 
				(n.isNull() ? null : n.asText()) 
				: def;
	}
	
	/**
	 * @param src
	 * @param name
	 * @return true only if the given field exists and is not set to null
	 */
	public static boolean notNull(JsonNode src, String name)
	{
		JsonNode jsonNode = src.get(name);
		return jsonNode != null && !jsonNode.isNull();
	}
	
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

	public static String serializeHumanReadable(JsonNode node)
	{
		try
		{
			return Constants.MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(node);
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
	
	public static JsonNode toJsonNode(Object value)
	{
		return Constants.MAPPER.convertValue(value, JsonNode.class);
	}
}
