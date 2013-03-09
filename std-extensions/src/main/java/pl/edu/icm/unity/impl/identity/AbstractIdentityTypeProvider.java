/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licensing information.
 */
package pl.edu.icm.unity.impl.identity;

import pl.edu.icm.unity.types.basic.IdentityTypeDefinition;

/**
 * Base identity type definition. Provides equals and hashcode based on id and default implementations of
 * toPrettyString and toString.
 * @author K. Benedyczak
 */
public abstract class AbstractIdentityTypeProvider implements IdentityTypeDefinition
{
	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toPrettyString(String from)
	{
		return "[" + getId() + "] " + toPrettyStringNoPrefix(from);
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
