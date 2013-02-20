/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licensing information.
 */
package pl.edu.icm.unity.stdext.attr;

import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.exceptions.IllegalAttributeValueException;
import pl.edu.icm.unity.types.AttributeValueSyntax;

/**
 * String attribute value syntax. Can have regular expression
 * defined to limit contents. Also can have min and max length defined.
 * @author K. Benedyczak
 */
@Component
public class StringAttributeSyntax implements AttributeValueSyntax<String>
{
	public static final String ID = "string";
	private static final ObjectMapper json = new ObjectMapper();
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
	public JsonNode getSerializedConfiguration()
	{
		ObjectNode main = json.createObjectNode();
		main.put("regexp", getRegexp());
		main.put("minLength", getMinLength());
		main.put("maxLength", getMaxLength());
		return main;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setSerializedConfiguration(JsonNode json)
	{
		setRegexp(json.get("regexp").asText());
		setMinLength(json.get("minLength").asInt());
		setMaxLength(json.get("maxLength").asInt());
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
		if (value.length() < maxLength)
			throw new IllegalAttributeValueException("Value length (" + value.length() 
					+ ") is too big, must be not greater then " + maxLength);
		if (pattern != null)
			if (!pattern.matcher(value).matches())
				throw new IllegalAttributeValueException("Value must match the " +
						"regualr expression: " + getRegexp());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean areEqual(String value, String another)
	{
		return value == null ? value == another : value.equals(another);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public byte[] serialize(String value)
	{
		return value.getBytes();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String deserialize(byte[] raw)
	{
		return new String(raw);
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
		this.maxLength = maxLength;
	}
}
