/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE file for licensing information.
 */
package pl.edu.icm.unity.stdext.attr;

import java.util.Objects;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.engine.api.attributes.AbstractAttributeValueSyntaxFactory;
import pl.edu.icm.unity.engine.api.attributes.AttributeValueSyntax;
import pl.edu.icm.unity.exceptions.IllegalAttributeValueException;

/**
 * Boolean attribute value syntax.
 * @author K. Benedyczak
 */
public class BooleanAttributeSyntax implements AttributeValueSyntax<Boolean>
{
	public static final String ID = "boolean";
	
	public BooleanAttributeSyntax()
	{
	}
	
	@Override
	public String getValueSyntaxId()
	{
		return ID;
	}

	@Override
	public JsonNode getSerializedConfiguration()
	{
		ObjectNode main = Constants.MAPPER.createObjectNode();
		return main;
	}

	@Override
	public void setSerializedConfiguration(JsonNode jsonN)
	{
	}

	@Override
	public void validate(Boolean value) throws IllegalAttributeValueException
	{
	}

	@Component
	public static class Factory extends AbstractAttributeValueSyntaxFactory<Boolean>
	{
		public Factory()
		{
			super(BooleanAttributeSyntax.ID, BooleanAttributeSyntax::new);
		}
	}

	@Override
	public boolean areEqual(Boolean value, Object another)
	{
		return Objects.equals(value, another);
	}

	@Override
	public int hashCode(Object value)
	{
		return value.hashCode();
	}

	@Override
	public Boolean convertFromString(String stringRepresentation)
	{
		return Boolean.valueOf(stringRepresentation);
	}

	@Override
	public String convertToString(Boolean value)
	{
		return String.valueOf(value);
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
}
