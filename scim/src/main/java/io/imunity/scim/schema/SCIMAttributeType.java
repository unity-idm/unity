/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.scim.schema;

import java.util.Optional;
import java.util.stream.Stream;

public enum SCIMAttributeType
{
	STRING("string"), BOOLEAN("boolean"),
	DATETIME("dateTime"),
	REFERENCE("reference"), COMPLEX("complex");

	private String name;

	SCIMAttributeType(final String name)
	{
		this.name = name;
	}

	public String getName()
	{
		return name;
	}

	public static SCIMAttributeType fromName(String type) throws UnsupportedAttributeTypeException
	{
		Optional<SCIMAttributeType> atype = Stream.of(SCIMAttributeType.values()).filter(t -> t.name.equals(type))
				.findAny();
		if (atype.isEmpty())
			throw new UnsupportedAttributeTypeException("Attribute type " + type + " is unsupported");
		return atype.get();
	}

}