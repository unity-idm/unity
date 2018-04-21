/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.stdext.attr;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.engine.api.attributes.AbstractAttributeValueSyntaxFactory;
import pl.edu.icm.unity.engine.api.attributes.AttributeValueSyntax;
import pl.edu.icm.unity.exceptions.IllegalAttributeValueException;
import pl.edu.icm.unity.exceptions.WrongArgumentException;


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
	public ObjectNode getSerializedConfiguration()
	{
		ObjectNode main = Constants.MAPPER.createObjectNode();
		main.put("min", getMin());
		main.put("max", getMax());
		return main;
	}

	@Override
	public void setSerializedConfiguration(JsonNode jsonN)
	{
		min = jsonN.get("min").asLong();
		max = jsonN.get("max").asLong();		
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

	public long getMin()
	{
		return min;
	}

	public void setMin(long min) throws WrongArgumentException
	{
		if (min > max)
			throw new WrongArgumentException("Minimum must not be less then the maximum");
		this.min = min;
	}

	public long getMax()
	{
		return max;
	}

	public void setMax(long max) throws WrongArgumentException
	{
		if (max < min)
			throw new WrongArgumentException("Maximum must not be less then the minimum");
		this.max = max;
	}

	@Override
	public boolean isEmailVerifiable()
	{
		return false;
	}

	@Override
	public boolean isUserVerifiable()
	{
		return false;
	}

	@Override
	public Long convertFromString(String stringRepresentation)
	{
		return Long.parseLong(stringRepresentation);
	}

	@Override
	public String convertToString(Long value)
	{
		return value.toString();
	}
	
	
	@Component
	public static class Factory extends AbstractAttributeValueSyntaxFactory<Long>
	{
		public Factory()
		{
			super(IntegerAttributeSyntax.ID, IntegerAttributeSyntax::new);
		}
	}
}
