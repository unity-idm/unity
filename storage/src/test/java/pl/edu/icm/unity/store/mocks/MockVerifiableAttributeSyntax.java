/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.mocks;

import java.nio.charset.StandardCharsets;

import org.springframework.stereotype.Component;

import pl.edu.icm.unity.exceptions.IllegalAttributeValueException;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.types.basic.AttributeValueSyntax;

@Component
public class MockVerifiableAttributeSyntax implements AttributeValueSyntax<String>
{
	public static final String ID = "verifiableMockString";

	@Override
	public String getSerializedConfiguration() throws InternalException
	{
		return "";
	}

	@Override
	public void setSerializedConfiguration(String json) throws InternalException
	{
	}

	@Override
	public String getValueSyntaxId()
	{
		return ID;
	}

	@Override
	public void validate(String value) throws IllegalAttributeValueException
	{
	}

	@Override
	public boolean areEqual(String value, Object another)
	{
		return value.equals(another);
	}

	@Override
	public int hashCode(Object value)
	{
		return value.hashCode();
	}

	@Override
	public byte[] serialize(String value) throws InternalException
	{
		return value.getBytes(StandardCharsets.UTF_8);
	}

	@Override
	public Object serializeSimple(String value) throws InternalException
	{
		return value;
	}

	@Override
	public String deserializeSimple(Object value) throws InternalException
	{
		return value.toString();
	}

	@Override
	public String deserialize(byte[] raw) throws InternalException
	{
		return new String(raw, StandardCharsets.UTF_8);
	}

	@Override
	public boolean isVerifiable()
	{
		return true;
	}

	@Override
	public String convertFromString(String stringRepresentation)
			throws IllegalAttributeValueException
	{
		return stringRepresentation;
	}

}
