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
import pl.edu.icm.unity.types.basic.GroupContents;

import com.vaadin.ui.ComboBox;


/**
 * Combo box allowing to choose a group. This components can automatically populate the combobox 
 * with subgroups of a given group, both immediate and recursive.
 * @author K. Benedyczak
 */
public class GroupComboBox extends ComboBox
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, GroupComboBox.class);
	private GroupsManagement groupsMan;
	private List<String> fixedGroups;
	
	public GroupComboBox(String caption, Collection<String> groups)
	{
		setCaption(caption);
		this.fixedGroups = new ArrayList<>(groups);
		setNullSelectionAllowed(false);
	}

	public GroupComboBox(String caption, GroupsManagement groupsMan)
	{
		setCaption(caption);
		this.groupsMan = groupsMan;
		setNullSelectionAllowed(false);
	}

	public void setInput(String rootGroup, boolean recursive, boolean inclusive)
	{
		removeAllItems();
		if (groupsMan != null)
		{
			fixedGroups = new ArrayList<String>();
			getGroups(rootGroup, recursive, fixedGroups);
		}
		if (inclusive && !fixedGroups.contains(rootGroup))
			fixedGroups.add(rootGroup);
		if (!inclusive && fixedGroups.contains(rootGroup))
			fixedGroups.remove(rootGroup);
		Collections.sort(fixedGroups);
		for (String group: fixedGroups)
			addItem(group);
		if (!fixedGroups.isEmpty())
			select(fixedGroups.get(0));
	}
	
	public List<String> getGroups()
	{
		return new ArrayList<>(fixedGroups);
	}
	
	private void getGroups(String group, boolean recursive, List<String> groups)
	{
		getGroups(group, recursive, groups, groupsMan);
	}
	
	public static void getGroups(String group, boolean recursive, List<String> groups, GroupsManagement groupsMan)
	{
		GroupContents contents;
		try
		{
			contents = groupsMan.getContents(group, GroupContents.GROUPS);
			for (String subgroup: contents.getSubGroups())
			{
				groups.add(subgroup);
				if (recursive)
					getGroups(subgroup, recursive, groups, groupsMan);
			}
		} catch (Exception e)
		{
			log.warn("Can't read groups for combo box", e);
		}
	}
}
