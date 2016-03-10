/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.log4j.Logger;

import pl.edu.icm.unity.server.api.GroupsManagement;
import pl.edu.icm.unity.server.utils.Log;


/**
 * Utilities for group selection components.
 * @author K. Benedyczak
 */
public abstract class GroupSelectionUtils
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, GroupSelectionUtils.class);

	static List<String> establishGroups(String rootGroup, boolean inclusive, GroupsManagement groupsMan,
			Collection<String> presetGroups)
	{
		List<String> fixedGroups = new ArrayList<String>();
		if (groupsMan != null)
		{
			fixedGroups = new ArrayList<String>();
			getGroups(rootGroup, fixedGroups, groupsMan);
		} else
		{
			fixedGroups.addAll(presetGroups);
		}
		
		if (inclusive && !fixedGroups.contains(rootGroup))
			fixedGroups.add(rootGroup);
		if (!inclusive && fixedGroups.contains(rootGroup))
			fixedGroups.remove(rootGroup);
		Collections.sort(fixedGroups);
		return fixedGroups;
	}
	
	private static void getGroups(String group, List<String> groups, GroupsManagement groupsMan)
	{
		try
		{
			groups.addAll(groupsMan.getChildGroups(group));
		} catch (Exception e)
		{
			log.warn("Can't read groups for combo box", e);
		}
	}
}
