/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licensing information.
 */
package pl.edu.icm.unity.saml.idp;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Chooses a correct group for the requester.
 * @author K. Benedyczak
 */
public class GroupChooser
{
	private String defaultGroup;
	private Map<String, String> groupMappings;
	
	public GroupChooser(SamlIdpProperties config)
	{
		defaultGroup = config.getValue(SamlIdpProperties.DEFAULT_GROUP);
		Set<String> keys = config.getStructuredListKeys(SamlIdpProperties.GROUP_PFX);
		groupMappings = new HashMap<String, String>(keys.size());
		for (String key: keys)
		{
			String target = config.getValue(key+SamlIdpProperties.GROUP_TARGET);
			String group = config.getValue(key+SamlIdpProperties.GROUP);
			groupMappings.put(target, group);
		}
	}
	
	public String chooseGroup(String requester)
	{
		String group = groupMappings.get(requester);
		return group == null ? defaultGroup : group;
	}
}
