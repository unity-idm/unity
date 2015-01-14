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
import pl.edu.icm.unity.exceptions.IllegalAttributeValueException;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.types.basic.AttributeValueSyntax;


/**
 * Floating point number attribute value syntax. Allows for specifying upper and lower bounds.
 * @author K. Benedyczak
 */
public class FloatingPointAttributeSyntax implements AttributeValueSyntax<Double>
{
	public static final String ID = "floatingPoint";
	private double min = Double.MIN_VALUE;
	private double max = Double.MAX_VALUE;
	
	@Override
	public String getSerializedConfiguration() throws InternalException
	{
		ObjectNode main = Constants.MAPPER.createObjectNode();
		main.put("min", getMin());
		main.put("max", getMax());
		try
		{
			return Constants.MAPPER.writeValueAsString(main);
		} catch (JsonProcessingException e)
		{
			throw new InternalException("Can't serialize FloatingPointAttributeSyntax to JSON", e);
		}
	}

	@Override
	public void setSerializedConfiguration(String json) throws InternalException
	{
		JsonNode jsonN;
		try
		{
			jsonN = Constants.MAPPER.readTree(json);
		} catch (Exception e)
		{
			throw new InternalException("Can't deserialize FloatingPointAttributeSyntax from JSON", e);
		}
		
		min = jsonN.get("min").asDouble();
		max = jsonN.get("max").asDouble();		
	}

	@Override
	public String getValueSyntaxId()
	{
		return ID;
	}

	@Override
	public void validate(Double value) throws IllegalAttributeValueException
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
	public boolean areEqual(Double value, Object another)
	{
		return value == null ? null == another : value.equals(another);
	}

	@Override
	public int hashCode(Object value)
	{
		return value.hashCode();
	}

	@Override
	public byte[] serialize(Double value)
	{
	        ByteBuffer bb = ByteBuffer.allocate(8);  
	        return bb.putDouble(value).array(); 
	}

	@Override
	public Double deserialize(byte[] raw)
	{
		ByteBuffer bb = ByteBuffer.wrap(raw);
		return bb.getDouble();
	}

	public double getMin()
	{
		return min;
	}

	public void setMin(double min) throws WrongArgumentException
	{
		if (min > max)
			throw new WrongArgumentException("Minimum must not be less then the maximum");
		this.min = min;
	}

	public double getMax()
	{
		return max;
	}

	public void setMax(double max) throws WrongArgumentException
	{
		if (max < min)
			throw new WrongArgumentException("Maximum must not be less then the minimum");
		this.max = max;
	}

	@Override
	public Object serializeSimple(Double value) throws InternalException
	{
		return value;
	}

	@Override
	public boolean hasValuesVerifiable()
	{
		return false;
	}
}
