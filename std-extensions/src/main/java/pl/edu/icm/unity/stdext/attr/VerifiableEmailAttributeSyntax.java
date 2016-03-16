/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.stdext.attr;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.exceptions.IllegalAttributeValueException;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.stdext.utils.EmailUtils;
import pl.edu.icm.unity.types.basic.AttributeValueSyntax;
import pl.edu.icm.unity.types.basic.VerifiableEmail;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Verifiable email attribute value syntax.
 * @author P. Piernik
 */
public class VerifiableEmailAttributeSyntax implements AttributeValueSyntax<VerifiableEmail> 
{
	public static final String ID = "verifiableEmail";
	
	@Override
	public String getValueSyntaxId()
	{
		return ID;
	}

	@Override
	public boolean areEqual(VerifiableEmail value, Object another)
	{
		return value.equals(another);
	}

	@Override
	public int hashCode(Object value)
	{
		return value.hashCode();
	}

	@Override
	public byte[] serialize(VerifiableEmail value) throws InternalException
	{
		JsonNode main = value.toJson();
		try
		{
			return Constants.MAPPER.writeValueAsString(main).getBytes(StandardCharsets.UTF_8);
		} catch (JsonProcessingException e)
		{
			throw new InternalException("Can't serialize VerifiableEmail to JSON", e);
		}
	}

	@Override
	public Object serializeSimple(VerifiableEmail value)
	{
		return value.toJson();
	}

	@Override
	public VerifiableEmail deserializeSimple(Object value)
	{
		if (value instanceof Map)
		{
			JsonNode node = Constants.MAPPER.convertValue(value, JsonNode.class);
			return new VerifiableEmail(node);
		}
		throw new InternalException("Value must be json encoded and is " + value.getClass() + 
				"\n" + value);
	}


	@Override
	public VerifiableEmail deserialize(byte[] raw) throws InternalException
	{
		JsonNode jsonN;
		try
		{
			jsonN = Constants.MAPPER.readTree(new String(raw, StandardCharsets.UTF_8));
		} catch (Exception e)
		{
			throw new InternalException("Can't deserialize VerifiableEmail from JSON", e);
		}
		return new VerifiableEmail(jsonN);
	}

	@Override
	public void validate(VerifiableEmail value) throws IllegalAttributeValueException
	{
		if (value == null)
			throw new IllegalAttributeValueException("null value is illegal");
		String error = EmailUtils.validate(value.getValue());
		if (error != null)
			throw new IllegalAttributeValueException(value.getValue() + ": " + error);
	}

	@Override
	public String getSerializedConfiguration() throws InternalException
	{
		return "{}";
	}

	@Override
	public void setSerializedConfiguration(String json) throws InternalException
	{
	      //OK
	}

	@Override
	public boolean isVerifiable()
	{
		return true;
	}

	@Override
	public VerifiableEmail convertFromString(String stringRepresentation)
			throws IllegalAttributeValueException
	{
		VerifiableEmail ret = EmailUtils.convertFromString(stringRepresentation);
		validate(ret);
		return ret;
	}
}
