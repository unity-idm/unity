/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types;

import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;

import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.exceptions.InternalException;

/**
 *
 * General purpose factory to convert Unity types from json string to object
 * back and fourth.
 *
 * @author R. Krysinski
 */
public class UnityTypesFactory
{
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
