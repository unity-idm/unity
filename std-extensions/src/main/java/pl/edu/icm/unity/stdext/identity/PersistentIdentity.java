/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licensing information.
 */
package pl.edu.icm.unity.stdext.identity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Component;

import pl.edu.icm.unity.exceptions.IllegalIdentityValueException;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.IdentityRepresentation;

/**
 * Identity type definition holding a persistent id. It is associated with each and every entity. 
 * Can not be removed, without removing the whole containing entity.
 * @author K. Benedyczak
 */
@Component
public class PersistentIdentity extends AbstractIdentityTypeProvider
{
	public static final String ID = "persistent";
	private static final List<Attribute<?>> empty = Collections.unmodifiableList(new ArrayList<Attribute<?>>(0));
	
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
		return "Persistent id";
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Set<AttributeType> getAttributesSupportedForExtraction()
	{
		return Collections.emptySet();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void validate(String value) throws IllegalIdentityValueException
	{
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
		return empty; 
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
		return true;
	}
	
	@Override
	public String toExternalForm(String realm, String target, String inDbValue, String comparableValue)
	{
		return inDbValue;
	}

	@Override
	public IdentityRepresentation createNewIdentity(String realm, String target, String inDbValue)
	{
		String value = inDbValue == null ? UUID.randomUUID().toString() : inDbValue;
		return new IdentityRepresentation(value, value);
	}

	@Override
	public boolean isTargeted()
	{
		return false;
	}

	@Override
	public String toExternalFormNoContext(String inDbValue, String comparableValue)
	{
		return inDbValue;
	}

	@Override
	public boolean isExpired(IdentityRepresentation representation)
	{
		return false;
	}
}
