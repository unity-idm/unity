/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licensing information.
 */
package pl.edu.icm.unity.stdext.identity;

import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Component;

import eu.emi.security.authn.x509.impl.X500NameUtils;

import pl.edu.icm.unity.exceptions.IllegalIdentityValueException;
import pl.edu.icm.unity.types.Attribute;
import pl.edu.icm.unity.types.IdentityTypeDefinition;

/**
 * X.500 identity type definition
 * @author K. Benedyczak
 */
@Component
public class X500Identity implements IdentityTypeDefinition
{
	public static final String ID = "x500Name";
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getId()
	{
		return ID;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getDefaultDescription()
	{
		return "X.500 Distinguished Name";
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<String> getAttributesSupportedForExtraction()
	{
		return Collections.emptyList();
		//throw new RuntimeException("NOT implemented"); // TODO Auto-generated method stub
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void validate(String value) throws IllegalIdentityValueException
	{
		try
		{
			X500NameUtils.getX500Principal(value);
		} catch (Exception e)
		{
			throw new IllegalIdentityValueException("DN is invalid: " + 
					e.getMessage());
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getComparableValue(String from)
	{
		return X500NameUtils.getComparableForm(from);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<Attribute<?>> extractAttributes(String from, List<String> toExtract)
	{
		throw new RuntimeException("NOT implemented"); // TODO Auto-generated method stub
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toPrettyString(String from)
	{
		return "[X.500 DN] " + toPrettyStringNoPrefix(from);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toPrettyStringNoPrefix(String from)
	{
		return X500NameUtils.getReadableForm(from);
	}


	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString(String from)
	{
		return toPrettyString(from);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString()
	{
		return getId();
	}

}
