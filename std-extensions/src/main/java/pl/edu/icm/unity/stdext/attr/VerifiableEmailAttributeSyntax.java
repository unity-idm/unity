/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.stdext.attr;

import java.util.regex.Pattern;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.exceptions.IllegalAttributeValueException;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.types.basic.AttributeValueSyntax;
import pl.edu.icm.unity.types.confirmation.ConfirmationInfo;

/**
 * Verifiable email attribute value syntax.
 * @author P. Piernik
 */
public class VerifiableEmailAttributeSyntax implements AttributeValueSyntax<VerifiableEmail> 
{
	public static final String ID = "verifiableEmail";
	private final int MAX_LENGTH = 80;
	private final String EMAIL_REGEXP = "[^@]+@.+\\..+";
	private Pattern pattern = null;
	
	
	public VerifiableEmailAttributeSyntax()
	{
		pattern = Pattern.compile(EMAIL_REGEXP);
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
		ObjectNode main = Constants.MAPPER.createObjectNode();
		main.put("value",value.getValue());
		main.put("confirmationData", value.getConfirmationInfo().getSerializedConfiguration());
		try
		{
			return Constants.MAPPER.writeValueAsString(main).getBytes();
		} catch (JsonProcessingException e)
		{
			throw new InternalException("Can't serialize VerifiableEmail to JSON", e);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Object serializeSimple(VerifiableEmail value) throws InternalException
	{
		return serialize(value);
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
			jsonN = Constants.MAPPER.readTree(new String(raw));
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
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void validate(VerifiableEmail value) throws IllegalAttributeValueException
	{
		if (value == null || value.getValue() == null)
			throw new IllegalAttributeValueException("null value is illegal");
		if (value.getValue().length() > MAX_LENGTH)
			throw new IllegalAttributeValueException("Value length (" + value.getValue().length() 
					+ ") is too big, must be not greater than " + MAX_LENGTH);
		if (!pattern.matcher(value.getValue()).matches())
			throw new IllegalAttributeValueException("Value must match the " +
						"regualr expression: " + EMAIL_REGEXP);
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
		VerifiableEmail ret = new VerifiableEmail(stringRepresentation);
		validate(ret);
		return ret;
	}
}
