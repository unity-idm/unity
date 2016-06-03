/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licensing information.
 */
package pl.edu.icm.unity.stdext.identity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.engine.api.identity.IdentityTypeDefinition;
import pl.edu.icm.unity.exceptions.IllegalIdentityValueException;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.IdentityParam;

/**
 * Base identity type definition. Provides equals and hashcode based on id and default implementations of
 * toPrettyString and toString.
 * @author K. Benedyczak
 */
public abstract class AbstractIdentityTypeProvider implements IdentityTypeDefinition
{
	public static final List<Attribute> EMPTY_ATTRS = Collections.unmodifiableList(new ArrayList<>(0));
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toPrettyString(IdentityParam from)
	{
		return "[" + getId() + "] " + toPrettyStringNoPrefix(from);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString(IdentityParam from)
	{
		return toPrettyString(from);
	}
	
	/**
	 * Delegated to {@link #toPrettyStringNoPrefix(String)}
	 * @param msg
	 * @param from
	 * @return
	 */
	@Override
	public String toHumanFriendlyString(MessageSource msg, IdentityParam from)
	{
		return toPrettyStringNoPrefix(from);
	}
	
	/**
	 * Most of the implementations are removable
	 */
	@Override
	public boolean isRemovable()
	{
		return true;
	}
	
	@Override
	public IdentityParam convertFromString(String stringRepresentation, String remoteIdp, 
			String translationProfile) throws IllegalIdentityValueException
	{
		return new IdentityParam(getId(), stringRepresentation, remoteIdp, translationProfile);
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
	public int hashCode()
	{
		return getId().hashCode();
	}
	
	@Override
	public boolean equals(Object anotherO)
	{
		if (anotherO == null || !(anotherO instanceof IdentityTypeDefinition))
			return false;
		IdentityTypeDefinition another = (IdentityTypeDefinition) anotherO;
		return getId().equals(another.getId());
	}
}
