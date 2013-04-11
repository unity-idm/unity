/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.stdext.attr;

import java.nio.ByteBuffer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.exceptions.IllegalArgumentException;
import pl.edu.icm.unity.exceptions.IllegalAttributeValueException;
import pl.edu.icm.unity.exceptions.RuntimeEngineException;
import pl.edu.icm.unity.types.basic.AttributeValueSyntax;


/**
 * Integer attribute value syntax. Allows for specifying upper and lower bounds.
 * @author K. Benedyczak
 */
public class IntegerAttributeSyntax implements AttributeValueSyntax<Long>
{
	public static final String ID = "integer";
	private long min = Long.MIN_VALUE;
	private long max = Long.MAX_VALUE;
	
	@Override
	public String getSerializedConfiguration()
	{
		ObjectNode main = Constants.MAPPER.createObjectNode();
		main.put("min", getMin());
		main.put("max", getMax());
		try
		{
			return Constants.MAPPER.writeValueAsString(main);
		} catch (JsonProcessingException e)
		{
			throw new RuntimeEngineException("Can't serialize IntegerAttributeSyntax to JSON", e);
		}
	}

	@Override
	public void setSerializedConfiguration(String json)
	{
		JsonNode jsonN;
		try
		{
			jsonN = Constants.MAPPER.readTree(json);
		} catch (Exception e)
		{
			throw new RuntimeEngineException("Can't deserialize IntegerAttributeSyntax from JSON", e);
		}
		setMin(jsonN.get("min").asLong());
		setMax(jsonN.get("max").asLong());		
	}

	@Override
	public String getValueSyntaxId()
	{
		return ID;
	}

	@Override
	public void validate(Long value) throws IllegalAttributeValueException
	{
		if (value == null)
			throw new IllegalAttributeValueException("null value is illegal");
		if (value < min)
			throw new IllegalAttributeValueException("Value (" + value 
					+ ") is too small, must be at least " + min);
		if (value > max)
			throw new IllegalAttributeValueException("Value (" + value 
					+ ") is too big, must be not greater than " + max);
	}

	@Override
	public boolean areEqual(Long value, Object another)
	{
		return value == null ? null == another : value.equals(another);
	}

	@Override
	public int hashCode(Object value)
	{
		return value.hashCode();
	}

	@Override
	public byte[] serialize(Long value)
	{
	        ByteBuffer bb = ByteBuffer.allocate(8);  
	        return bb.putLong(value).array(); 
	}

	@Override
	public Long deserialize(byte[] raw)
	{
		ByteBuffer bb = ByteBuffer.wrap(raw);
		return bb.getLong();
	}

	public long getMin()
	{
		return min;
	}

	public void setMin(long min)
	{
		if (min > max)
			throw new IllegalArgumentException("Minimum must not be less then the maximum");
		this.min = min;
	}

	public long getMax()
	{
		return max;
	}

	public void setMax(long max)
	{
		if (max < min)
			throw new IllegalArgumentException("Maximum must not be less then the minimum");
		this.max = max;
	}
}
