/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.stdext.attr;

import pl.edu.icm.unity.engine.api.attributes.AttributeValueSyntax;
import pl.edu.icm.unity.exceptions.InternalException;

/**
 * Common code for string based attribute syntax classes.
 * @author K. Benedyczak
 */
public abstract class AbstractStringAttributeSyntax implements AttributeValueSyntax<String>
{
	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean areEqual(String value, Object another)
	{
		return value == null ? null == another : value.equals(another);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int hashCode(Object value)
	{
		return value.hashCode();
	}

	@Override
	public String serializeSimple(String value) throws InternalException
	{
		return value;
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

	@Override
	public String convertFromString(String stringRepresentation)
	{
		return stringRepresentation;
	}
	
	@Override
	public String convertToString(String value)
	{
		return value;
	}
}
