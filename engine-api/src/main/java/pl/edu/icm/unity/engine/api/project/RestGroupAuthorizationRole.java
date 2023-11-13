/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.api.project;

/**
 * Rest group authorization role with description
 * 
 * @author P.Piernik
 *
 */
public enum RestGroupAuthorizationRole
{
	manager("Managments of projects ");
	
	private String description;

	RestGroupAuthorizationRole(String description)
	{
		this.description = description;

	}

	public String getDescription()
	{
		return description;
	}
}
