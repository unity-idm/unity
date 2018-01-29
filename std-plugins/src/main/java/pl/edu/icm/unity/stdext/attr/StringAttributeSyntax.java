/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licensing information.
 */
package pl.edu.icm.unity.stdext.attr;

import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.engine.api.attributes.AbstractAttributeValueSyntaxFactory;
import pl.edu.icm.unity.exceptions.IllegalAttributeValueException;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.exceptions.WrongArgumentException;

/**
 * String attribute value syntax. Can have regular expression
 * defined to limit contents. Also can have min and max length defined.
 * @author K. Benedyczak
 */
public class StringAttributeSyntax extends AbstractStringAttributeSyntax
{
	public static final String ID = "string";
	private int minLength = 0;
	private int maxLength = 10240;
	private Pattern pattern = null;
	
	
	public StringAttributeSyntax()
	{
	}
	
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
	 * @throws InternalException 
	 */
	@Override
	public JsonNode getSerializedConfiguration()
	{
		ObjectNode main = Constants.MAPPER.createObjectNode();
		main.put("regexp", getRegexp());
		main.put("minLength", getMinLength());
		main.put("maxLength", getMaxLength());
		return main;
	}

	/**
	 * {@inheritDoc}
	 * @throws InternalException 
	 */
	@Override
	public void setSerializedConfiguration(JsonNode jsonN)
	{
		setRegexp(jsonN.get("regexp").asText());
		minLength = jsonN.get("minLength").asInt();
		maxLength = jsonN.get("maxLength").asInt();
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
					+ ") is too big, must be not greater than " + maxLength);
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
	 * @throws IllegalArgumentException 
	 */
	public void setMinLength(int minLength) throws WrongArgumentException
	{
		if (minLength > maxLength)
			throw new WrongArgumentException("Minimal string length must not be less then the maximal");
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
	 * @throws IllegalArgumentException 
	 */
	public void setMaxLength(int maxLength) throws WrongArgumentException
	{
		if (maxLength < minLength)
			throw new WrongArgumentException("Maximal string length must not be less then the minimal");
		this.maxLength = maxLength;
	}
	
	
	@Component
	public static class Factory extends AbstractAttributeValueSyntaxFactory<String>
	{
		public Factory()
		{
			super(StringAttributeSyntax.ID, StringAttributeSyntax::new);
		}
	}
}
