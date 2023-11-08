/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.console.views.directory_browser.group_details;

import org.apache.logging.log4j.Logger;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.GroupsManagement;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

abstract class GroupSelectionUtils
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, GroupSelectionUtils.class);

	static List<String> establishGroups(String rootGroup, boolean inclusive, GroupsManagement groupsMan,
			Collection<String> presetGroups)
	{
		List<String> fixedGroups;
		if (groupsMan != null)
		{
			fixedGroups = new ArrayList<>();
			getGroups(rootGroup, fixedGroups, groupsMan);
		} else
		{
			fixedGroups = new ArrayList<>(presetGroups);
		}
		
		if (inclusive && !fixedGroups.contains(rootGroup))
			fixedGroups.add(rootGroup);
		if (!inclusive)
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
