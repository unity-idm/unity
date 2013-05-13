/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.groupbrowser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import pl.edu.icm.unity.server.api.GroupsManagement;
import pl.edu.icm.unity.types.basic.GroupContents;

import com.vaadin.ui.ComboBox;

/**
 * Combo box allowing to choose a group. This components can automatically populate the combobox 
 * with subgroups of a given group, both immediate and recursive.
 * @author K. Benedyczak
 */
public class GroupComboBox extends ComboBox
{
	private GroupsManagement groupsMan;
	
	public GroupComboBox(String caption, GroupsManagement groupsMan)
	{
		setCaption(caption);
		this.groupsMan = groupsMan;
		setNullSelectionAllowed(false);
	}

	public void setInput(String rootGroup, boolean recursive)
	{
		removeAllItems();
		List<String> groups = new ArrayList<String>();
		getGroups(rootGroup, recursive, groups);
		Collections.sort(groups);
		for (String group: groups)
			addItem(group);
		if (!groups.isEmpty())
			select(groups.get(0));
	}
	
	private void getGroups(String group, boolean recursive, List<String> groups)
	{
		GroupContents contents;
		try
		{
			contents = groupsMan.getContents(group, GroupContents.GROUPS);
			for (String subgroup: contents.getSubGroups())
			{
				groups.add(subgroup);
				if (recursive)
					getGroups(subgroup, recursive, groups);
			}
		} catch (Exception e)
		{
			//ignored - probably authZ error
		}
	}
}
