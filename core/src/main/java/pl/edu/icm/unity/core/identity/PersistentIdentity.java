/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licensing information.
 */
package pl.edu.icm.unity.core.identity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;

import eu.emi.security.authn.x509.impl.X500NameUtils;

import pl.edu.icm.unity.exceptions.IllegalIdentityValueException;
import pl.edu.icm.unity.types.Attribute;
import pl.edu.icm.unity.types.IdentityTypeDefinition;

/**
 * Identity type definition holding a persistent id. It is associated with each and every entity. 
 * Can not be removed, without removing the whole containing entity.
 * @author K. Benedyczak
 */
@Component
public class PersistentIdentity implements IdentityTypeDefinition
{
	public static final String ID = "persistent";
	private static final List<Attribute<?>> empty = Collections.unmodifiableList(new ArrayList<Attribute<?>>(0));
	
	public static String getNewId()
	{
		return UUID.randomUUID().toString();
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
		return "Persistent id";
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<String> getAttributesSupportedForExtraction()
	{
		return Collections.emptyList();
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
	public String getComparableValue(String from)
	{
		return from;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<Attribute<?>> extractAttributes(String from, List<String> toExtract)
	{
		return empty; 
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toPrettyString(String from)
	{
		return "[persistentId] " + toPrettyStringNoPrefix(from);
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

	@Override
	public boolean isSystem()
	{
		return true;
	}
}
