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
	manager("Group manager"),
	allowReDelegate("Group manager with re delegate privilages"), 
	allowReDelegateRecursive("Group manager with recursive redelegate  privilages"), 
	regular("Regular group member");

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
