/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licensing information.
 */
package pl.edu.icm.unity.stdext.attr;

import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.exceptions.IllegalArgumentException;
import pl.edu.icm.unity.exceptions.IllegalAttributeValueException;
import pl.edu.icm.unity.exceptions.RuntimeEngineException;

/**
 * String attribute value syntax. Can have regular expression
 * defined to limit contents. Also can have min and max length defined.
 * @author K. Benedyczak
 */
@Component
public class StringAttributeSyntax extends AbstractStringAttributeSyntax
{
	public static final String ID = "string";
	private int minLength = 0;
	private int maxLength = 10240;
	private Pattern pattern = null;
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getValueSyntaxId()
	{
		return ID;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getSerializedConfiguration()
	{
		ObjectNode main = Constants.MAPPER.createObjectNode();
		main.put("regexp", getRegexp());
		main.put("minLength", getMinLength());
		main.put("maxLength", getMaxLength());
		try
		{
			return Constants.MAPPER.writeValueAsString(main);
		} catch (JsonProcessingException e)
		{
			throw new RuntimeEngineException("Can't serialize StringAttributeSyntax to JSON", e);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setSerializedConfiguration(String jsonStr)
	{
		JsonNode jsonN;
		try
		{
			jsonN = Constants.MAPPER.readTree(jsonStr);
		} catch (Exception e)
		{
			throw new RuntimeEngineException("Can't deserialize StringAttributeSyntax from JSON", e);
		}
		setRegexp(jsonN.get("regexp").asText());
		setMinLength(jsonN.get("minLength").asInt());
		setMaxLength(jsonN.get("maxLength").asInt());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void validate(String value) throws IllegalAttributeValueException
	{
		if (value == null)
			throw new IllegalAttributeValueException("null value is illegal");
		if (value.length() < minLength)
			throw new IllegalAttributeValueException("Value length (" + value.length() 
					+ ") is too small, must be at least " + minLength);
		if (value.length() > maxLength)
			throw new IllegalAttributeValueException("Value length (" + value.length() 
					+ ") is too big, must be not greater then " + maxLength);
		if (pattern != null)
			if (!pattern.matcher(value).matches())
				throw new IllegalAttributeValueException("Value must match the " +
						"regualr expression: " + getRegexp());
	}


	/**
	 * @return the regexp
	 */
	public String getRegexp()
	{
		return pattern == null ? "" : pattern.pattern();
	}

	/**
	 * @param regexp the regexp to set
	 */
	public void setRegexp(String regexp)
	{
		this.pattern = "".equals(regexp) ? null : 
				Pattern.compile(regexp);
	}

	/**
	 * @return the minLength
	 */
	public int getMinLength()
	{
		return minLength;
	}

	/**
	 * @param minLength the minLength to set
	 */
	public void setMinLength(int minLength)
	{
		if (minLength > maxLength)
			throw new IllegalArgumentException("Minimal string length must not be less then the maximal");
		this.minLength = minLength;
	}

	/**
	 * @return the maxLength
	 */
	public int getMaxLength()
	{
		return maxLength;
	}

	/**
	 * @param maxLength the maxLength to set
	 */
	public void setMaxLength(int maxLength)
	{
		if (maxLength < minLength)
			throw new IllegalArgumentException("Maxmal string length must not be less then the minimal");
		this.maxLength = maxLength;
	}
}
