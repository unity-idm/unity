/*
 * Copyright (c) 2017 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.event;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Defines the well known phases of events.
 *
 * @author Roman Krysinski (roman@unity-idm.eu)
 */
public enum EventCategory
{
	PRE_INIT("pre-init"),
	POST_INIT("post-init"),
	TEST("test");
	
	private String phaseName;

	private EventCategory(String phaseName)
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
				.map(EventCategory::toString)
				.collect(Collectors.joining(", "));
	}
	
	public static EventCategory of(String name)
	{
		return Arrays.asList(values()).stream()
				.filter(phase -> phase.toString()
				.equalsIgnoreCase(name)).findFirst()
				.orElseThrow(() -> new IllegalArgumentException("No enum const InitializationPhase: " + name));
	}
}
