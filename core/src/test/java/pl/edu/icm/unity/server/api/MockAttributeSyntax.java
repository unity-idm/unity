/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.api;

import pl.edu.icm.unity.exceptions.IllegalAttributeValueException;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.types.basic.AttributeValueSyntax;


public class MockAttributeSyntax implements AttributeValueSyntax<String> {

	@Override
	public String getSerializedConfiguration()
	{
		return null;
	}

	@Override
	public void setSerializedConfiguration(String json)
	{
	}

	@Override
	public String getValueSyntaxId()
	{
		return "mock";
	}

	@Override
	public void validate(String value) throws IllegalAttributeValueException
	{
	}

	@Override
	public boolean areEqual(String value, Object another)
	{
		return value == null ? value ==another : value.equals(another);
	}

	@Override
	public int hashCode(Object value)
	{
		return value.hashCode();
	}

	@Override
	public byte[] serialize(String value)
	{
		return null;
	}

	@Override
	public String deserialize(byte[] raw)
	{
		return null;
	}

	@Override
	public Object serializeSimple(String value) throws InternalException
	{
		return null;
	}

	@Override
	public boolean hasValuesVerifiable()
	{
		return false;
	}
}