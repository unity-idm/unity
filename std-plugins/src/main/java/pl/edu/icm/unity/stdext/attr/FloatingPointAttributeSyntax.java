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
 * Floating point number attribute value syntax. Allows for specifying upper and lower bounds.
 * @author K. Benedyczak
 */
public class FloatingPointAttributeSyntax implements AttributeValueSyntax<Double>
{
	public static final String ID = "floatingPoint";
	private double min = Double.MIN_VALUE;
	private double max = Double.MAX_VALUE;
	
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
	public boolean isEmailVerifiable()
	{
		return false;
	}	

	@Override
	public Double convertFromString(String stringRepresentation)
	{
		return Double.parseDouble(stringRepresentation);
	}

	@Override
	public String convertToString(Double value)
	{
		return value.toString();
	}

	@Override
	public boolean isUserVerifiable()
	{
		return false;
	}
	
	
	@Component
	public static class Factory extends AbstractAttributeValueSyntaxFactory<Double>
	{
		public Factory()
		{
			super(FloatingPointAttributeSyntax.ID, FloatingPointAttributeSyntax::new);
		}
	}
}
