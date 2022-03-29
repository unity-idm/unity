/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.scim.schema;

enum SCIMAttributeReturned
{
	ALWAYS("always"), NEVER("never"), DEFAULT("default"), REQUEST("request");

	private String name;

	SCIMAttributeReturned(final String name)
	{
		this.name = name;
	}

	String getName()
	{
		return name;
	}

}