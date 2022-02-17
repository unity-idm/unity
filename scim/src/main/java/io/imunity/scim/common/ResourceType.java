/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.scim.common;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import io.imunity.scim.exception.SCIMException;
import io.imunity.scim.exception.SCIMException.ScimErrorType;

public enum ResourceType
{

	USER("User"),

	GROUP("Group"),

	SCHEMA("Schema");

	private String name;

	ResourceType(final String name)
	{
		this.name = name;
	}

	@JsonValue
	public String getName()
	{
		return name;
	}

	@JsonCreator
	public static ResourceType fromName(final String name) throws SCIMException
	{
		for (ResourceType resourceType : ResourceType.values())
		{
			if (resourceType.getName().equalsIgnoreCase(name))
			{
				return resourceType;
			}
		}

		throw new SCIMException(400, ScimErrorType.invalidSyntax, "Unknown SCIM resource type", null);
	}

}