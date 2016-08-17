/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.initializers;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Type of initializer.
 *
 * @author Roman Krysinski (roman@unity-idm.eu)
 */
public enum InitializerType
{
	JSON("json"),
	GROOVY("groovy");

	private String typeName;

	private InitializerType(String typeName)
	{
		this.typeName = typeName;
	}

	public static InitializerType of(String name)
	{
		return Arrays.asList(values())
				.stream()
				.filter(type -> type.name().equalsIgnoreCase(name))
				.findFirst()
				.orElseThrow(() -> {
					throw new IllegalArgumentException("No enum const InitializerType." + name);
				});
	}

	public static String typeNamesToString()
	{
		return Arrays.asList(values()).stream().map(InitializerType::typeName).collect(Collectors.joining(", "));
	}

	public String typeName()
	{
		return typeName;
	}

	public void setTypeName(String typeName)
	{
		this.typeName = typeName;
	}
}
