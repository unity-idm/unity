/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.initializers;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Defines the phases when particular initializer can be executed.
 *
 * @author Roman Krysinski (roman@unity-idm.eu)
 */
public enum InitializationPhase
{
	PRE_INIT("pre-init"),
	POST_INIT("post-init");
	
	private String phaseName;

	private InitializationPhase(String phaseName)
	{
		this.phaseName = phaseName;
	}
	
	@Override
	public String toString()
	{
		return phaseName;
	}

	public static String typeNamesToString()
	{
		return Arrays.asList(values()).stream()
				.map(InitializationPhase::toString)
				.collect(Collectors.joining(", "));
	}
	
	public static InitializationPhase of(String name)
	{
		return Arrays.asList(values()).stream()
				.filter(phase -> phase.toString()
				.equalsIgnoreCase(name)).findFirst()
				.orElseThrow(() -> new IllegalArgumentException("No enum const InitializationPhase: " + name));
	}
}
