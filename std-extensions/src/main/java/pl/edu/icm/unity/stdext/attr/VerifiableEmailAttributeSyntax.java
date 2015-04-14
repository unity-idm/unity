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
import pl.edu.icm.unity.types.confirmation.ConfirmationInfo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Verifiable email attribute value syntax.
 * @author P. Piernik
 */
public class VerifiableEmailAttributeSyntax implements AttributeValueSyntax<VerifiableEmail> 
{
	public static final String ID = "verifiableEmail";
	
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
	public boolean areEqual(VerifiableEmail value, Object another)
	{
		return value.equals(another);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int hashCode(Object value)
	{
		return value.hashCode();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public byte[] serialize(VerifiableEmail value) throws InternalException
	{
		JsonNode main = serialize2Json(value);
		try
		{
			return Constants.MAPPER.writeValueAsString(main).getBytes(StandardCharsets.UTF_8);
		} catch (JsonProcessingException e)
		{
			throw new InternalException("Can't serialize VerifiableEmail to JSON", e);
		}
	}

	private JsonNode serialize2Json(VerifiableEmail value)
	{
		ObjectNode main = Constants.MAPPER.createObjectNode();
		main.put("value",value.getValue());
		main.put("confirmationData", value.getConfirmationInfo().getSerializedConfiguration());
		return main;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Object serializeSimple(VerifiableEmail value) throws InternalException
	{
		return serialize2Json(value);
	}

	@Override
	public VerifiableEmail deserializeSimple(Object value) throws InternalException
	{
		if (value instanceof Map)
		{
			JsonNode node = Constants.MAPPER.convertValue(value, JsonNode.class);
			return deserializeFromJson(node);
		}
		throw new InternalException("Value must be json encoded and is " + value.getClass() + 
				"\n" + value);
	}


	/**
	 * {@inheritDoc}
	 */
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
		VerifiableEmail email = new VerifiableEmail();
		email.setValue(jsonN.get("value").asText());
		ConfirmationInfo confirmationData = new ConfirmationInfo();
		confirmationData.setSerializedConfiguration(jsonN.get("confirmationData").asText());
		email.setConfirmationInfo(confirmationData);
		return email;
	}

	private VerifiableEmail deserializeFromJson(JsonNode jsonN) throws InternalException
	{
		VerifiableEmail email = new VerifiableEmail();
		email.setValue(jsonN.get("value").asText());
		ConfirmationInfo confirmationData = new ConfirmationInfo();
		confirmationData.setSerializedConfiguration(jsonN.get("confirmationData").asText());
		email.setConfirmationInfo(confirmationData);
		return email;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void validate(VerifiableEmail value) throws IllegalAttributeValueException
	{
		if (value == null)
			throw new IllegalAttributeValueException("null value is illegal");
		String error = EmailUtils.validate(value.getValue());
		if (error != null)
			throw new IllegalAttributeValueException(error);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getSerializedConfiguration() throws InternalException
	{
		return "{}";
	}

	/**
	 * {@inheritDoc}
	 */
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
