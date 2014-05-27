/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licensing information.
 */
package pl.edu.icm.unity.stdext.identity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

import pl.edu.icm.unity.exceptions.IllegalIdentityValueException;
import pl.edu.icm.unity.stdext.attr.StringAttribute;
import pl.edu.icm.unity.stdext.attr.StringAttributeSyntax;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.AttributeVisibility;

/**
 * Simple username identity type definition
 * @author K. Benedyczak
 */
@Component
public class UsernameIdentity extends AbstractStaticIdentityTypeProvider
{
	public static final String ID = "userName";
	private static Set<AttributeType> EXTRACTED;
	private static final String EXTRACTED_NAME = "uid";
	
	static 
	{
		EXTRACTED = new HashSet<AttributeType>(1);
		EXTRACTED.add(new AttributeType(EXTRACTED_NAME, new StringAttributeSyntax(), "User identifier"));
		EXTRACTED = Collections.unmodifiableSet(EXTRACTED);
	}
	
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
		return "Username";
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Set<AttributeType> getAttributesSupportedForExtraction()
	{
		return EXTRACTED;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void validate(String value) throws IllegalIdentityValueException
	{
		if (value == null || value.trim().length() == 0)
		{
			throw new IllegalIdentityValueException("Username must be non empty");
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getComparableValue(String from, String realm, String target)
	{
		return from;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<Attribute<?>> extractAttributes(String from, Map<String, String> toExtract)
	{
		String desiredName = toExtract.get(EXTRACTED_NAME);
		if (desiredName == null)
			return Collections.emptyList();
		Attribute<?> ret = new StringAttribute(desiredName, "/", AttributeVisibility.full, from);
		List<Attribute<?>> retL = new ArrayList<Attribute<?>>();
		retL.add(ret);
		return retL;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toPrettyStringNoPrefix(String from)
	{
		return from;
	}

	@Override
	public boolean isDynamic()
	{
		return false;
	}
}
