/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.groupdetails;

import java.util.UUID;

import pl.edu.icm.unity.types.basic.AttributeStatement;

/**
 * Decorated {@link AttributeStatement} with random unique id. Used as table entry: without it equals and hashcode
 * would not work as expected for equal {@link AttributeStatement}s as those do not contain any key. 
 * 
 * @author K. Benedyczak
 */
class AttrStatementWithId
{
	final AttributeStatement statement;
	final String id;
	
	AttrStatementWithId(AttributeStatement statement)
	{
		this.statement = statement;
		this.id = UUID.randomUUID().toString();
	}

	@Override
	public int hashCode()
	{
		return id.hashCode();
	}
	
	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		AttrStatementWithId other = (AttrStatementWithId) obj;
		if (id == null)
		{
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		return true;
	}

	@Override
	public String toString()
	{
		return statement.toString();
	}
}