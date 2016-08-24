/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.initializers;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Mode of initializer - options that define the behavior of initializer.
 *
 * @author Roman Krysinski (roman@unity-idm.eu)
 */
public enum InitializerMode
{
	CREATE_IF_NOT_EXISTS,
	OVERWRITE_CONTENT;

	public static String modesToString()
	{
		return Arrays.asList(values()).stream().map(InitializerMode::name).collect(Collectors.joining(", "));
	}

	public static InitializerMode of(String name)
	{
		return Arrays.asList(values())
				.stream()
				.filter(type -> type.name().equalsIgnoreCase(name))
				.findFirst()
				.orElseThrow(() -> new IllegalArgumentException("No enum const InitializerMode." + name));
	}
}
