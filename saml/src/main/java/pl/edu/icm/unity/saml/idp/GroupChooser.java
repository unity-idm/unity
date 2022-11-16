/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licensing information.
 */
package pl.edu.icm.unity.saml.idp;

import java.util.Map;

/**
 * Chooses a correct group for the requester.
 * @author K. Benedyczak
 */
public class GroupChooser
{
	private final String defaultGroup;
	private final Map<String, String> groupMappings;


	public GroupChooser(Map<String, String> groupMappings, String defaultGroup)
	{
		this.groupMappings = groupMappings;
		this.defaultGroup = defaultGroup;
	}
	
	public String chooseGroup(String requester)
	{
		String group = groupMappings.get(requester);
		return group == null ? defaultGroup : group;
	}
}
