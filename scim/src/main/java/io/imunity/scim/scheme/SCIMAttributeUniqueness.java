/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.scim.scheme;

public enum SCIMAttributeUniqueness
{

	NONE("none"), SERVER("server"), GLOBAL("global");

	private String name;

	SCIMAttributeUniqueness(final String name)
	{
		this.name = name;
	}

	public String getName()
	{
		return name;
	}
}