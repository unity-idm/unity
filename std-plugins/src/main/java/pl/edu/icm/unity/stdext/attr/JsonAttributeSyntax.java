/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.stdext.attr;

import java.util.Objects;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.base.Constants;
import pl.edu.icm.unity.base.attribute.IllegalAttributeValueException;
import pl.edu.icm.unity.base.exceptions.InternalException;
import pl.edu.icm.unity.engine.api.attributes.AbstractAttributeValueSyntaxFactory;
import pl.edu.icm.unity.engine.api.attributes.AttributeValueSyntax;

public class JsonAttributeSyntax implements AttributeValueSyntax<JsonNode>
{
	public static final String ID = "json";

	public JsonAttributeSyntax()
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
	public void validate(JsonNode value) throws IllegalAttributeValueException
	{
	}

	@Component
	public static class Factory extends AbstractAttributeValueSyntaxFactory<JsonNode>
	{
		public Factory()
		{
			super(JsonAttributeSyntax.ID, JsonAttributeSyntax::new);
		}
	}

	@Override
	public boolean areEqual(JsonNode value, Object another)
	{
		return Objects.equals(value, another);
	}

	@Override
	public int hashCode(Object value)
	{
		return value.hashCode();
	}

	@Override
	public JsonNode convertFromString(String stringRepresentation)
	{
		return fromString(stringRepresentation);
	}
	
	static JsonNode fromString(String stringRepresentation)
	{
		if (stringRepresentation == null)
			return NullNode.getInstance();

		try
		{
			return Constants.MAPPER.readTree(stringRepresentation);
		} catch (JsonProcessingException e)
		{
			throw new InternalException("Invalid Json string", e);
		}
	}

	@Override
	public String convertToString(JsonNode value)
	{
		return toString(value);
	}
	
	static String toString(JsonNode value)
	{
		if (value instanceof NullNode)
			return null; 
		try
		{
			return Constants.MAPPER.writeValueAsString(value);
		} catch (JsonProcessingException e)
		{
			throw new InternalException("Can not convert attribute value to string", e);
		}
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
