/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */


package pl.edu.icm.unity.engine.api.integration;

/**
 * Represent single integration event variable 
 * 
 * @author P. Piernik
 */
public class IntegrationEventVariable
{
	public final String name;
	public final String descriptionKey;
	
	public IntegrationEventVariable(String name, String descriptionKey)
	{
		this.name = name;
		this.descriptionKey = descriptionKey;
	}
}
