/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.db.json;

import com.fasterxml.jackson.core.JsonProcessingException;

import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.types.basic.AttributesClass;

/**
 * (De)serialization of {@link AttributesClass}es.
 * @author K. Benedyczak
 */
public class AttributeClassSerializer
{
	public static String serialize(AttributesClass ac) throws InternalException
	{
		try
		{
			return Constants.MAPPER.writeValueAsString(ac);
		} catch (JsonProcessingException e)
		{
			throw new InternalException("Can't serialize attribute class to JSON", e);
		}
	}

	public static AttributesClass deserialize(byte[] json) throws InternalException
	{
		try
		{
			return Constants.MAPPER.readValue(json, AttributesClass.class);
		} catch (Exception e)
		{
			throw new InternalException("Can't deserialize attribute class from JSON", e);
		}
	}
}
