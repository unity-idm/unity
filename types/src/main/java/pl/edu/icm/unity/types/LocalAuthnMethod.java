/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types;

import java.util.List;

/**
 * Defines mechanism used for local checking if authentication input is correct.
 * Here we have only the information about implementation which is useful for 
 * administrators.
 * 
 * @author K. Benedyczak
 */
public class LocalAuthnMethod
{
	private String id;
	private String description;
	private List<String> supportedEndpointClasses;
	
	
	public LocalAuthnMethod(String id, String description, List<String> supportedEndpointClasses)
	{
		this.id = id;
		this.description = description;
		this.supportedEndpointClasses = supportedEndpointClasses;
	}

	public String getId()
	{
		return id;
	}
	
	public String getDescription()
	{
		return description;
	}

	public List<String> getSupportedEndpointClasses()
	{
		return supportedEndpointClasses;
	}
}
