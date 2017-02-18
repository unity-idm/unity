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
	GROOVY("groovy");

	private String typeName;

	private InitializerType(String typeName)
	{
		this.typeName = typeName;
	}

	public static String typeNamesToString()
	{
		return Arrays.asList(values()).stream()
				.map(InitializerType::typeName)
				.collect(Collectors.joining(", "));
	}

	public String typeName()
	{
		return typeName;
	}
}
