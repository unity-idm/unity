/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.console.views.directory_browser.group_details;

import pl.edu.icm.unity.base.attribute.AttributeStatement;

import java.util.UUID;

class AttrStatementWithId
{
	public final AttributeStatement statement;
	public final String id;
	
	AttrStatementWithId(AttributeStatement statement)
	{
		this.statement = statement;
		this.id = UUID.randomUUID().toString();
	}

	String toShortString()
	{
		return "Assign " + (statement.dynamicAttributeMode() ? 
				statement.getDynamicAttributeType() :
					statement.getFixedAttribute().getName());
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
			return other.id == null;
		} else return id.equals(other.id);
	}

	@Override
	public String toString()
	{
		return statement.toString();
	}
}