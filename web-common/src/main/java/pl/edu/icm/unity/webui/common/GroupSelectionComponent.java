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

import com.vaadin.ui.CustomComponent;


/**
 * Base component for other which allows for selecting a group or groups. 
 * This base can automatically populate the component
 * with subgroups of a given group, both immediate and recursive.
 * @author K. Benedyczak
 */
public abstract class GroupSelectionComponent extends CustomComponent
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, GroupSelectionComponent.class);
	protected GroupsManagement groupsMan;
	protected List<String> fixedGroups;
	
	public GroupSelectionComponent(String caption, Collection<String> groups)
	{
		setCaption(caption);
		this.fixedGroups = new ArrayList<>(groups);
	}

	public GroupSelectionComponent(String caption, GroupsManagement groupsMan)
	{
		setCaption(caption);
		this.groupsMan = groupsMan;
	}

	protected void setInput(String rootGroup, boolean inclusive)
	{
		if (groupsMan != null)
		{
			fixedGroups = new ArrayList<String>();
			getGroups(rootGroup, fixedGroups);
		}
		if (inclusive && !fixedGroups.contains(rootGroup))
			fixedGroups.add(rootGroup);
		if (!inclusive && fixedGroups.contains(rootGroup))
			fixedGroups.remove(rootGroup);
		Collections.sort(fixedGroups);
	}
	
	public List<String> getAllGroups()
	{
		return new ArrayList<>(fixedGroups);
	}
	
	private void getGroups(String group, List<String> groups)
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
