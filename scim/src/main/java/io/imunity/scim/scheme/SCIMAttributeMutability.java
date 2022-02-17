/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.scim.scheme;

enum SCIMAttributeMutability
{
	READ_ONLY("readOnly"), READ_WRITE("readWrite"), IMMUTABLE("immutable"), WRITE_ONLY("writeOnly");

	private String name;

	SCIMAttributeMutability(final String name)
	{
		this.name = name;
	}

	String getName()
	{
		return name;
	}
}