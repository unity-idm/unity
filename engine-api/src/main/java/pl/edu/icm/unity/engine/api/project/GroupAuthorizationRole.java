/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.api.project;

/**
 * Group authorization role with description
 * 
 * @author P.Piernik
 *
 */
public enum GroupAuthorizationRole
{
	manager("Complete project management"),
	projectsAdmin("Complete project management plus ability to create sub-projects (if root project settings allows for that)"), 
	regular("No administration capabilities");

	private String description;

	GroupAuthorizationRole(String description)
	{
		this.description = description;

	}

	public String getDescription()
	{
		return description;
	}
}
