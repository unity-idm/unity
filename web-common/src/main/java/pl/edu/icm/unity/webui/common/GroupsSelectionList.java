/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import pl.edu.icm.unity.server.api.GroupsManagement;

import com.vaadin.ui.ListSelect;
import com.vaadin.ui.TwinColSelect;


/**
 * {@link ListSelect} allowing to choose a set of groups. This component can automatically populate the list 
 * with subgroups of a given group, both immediate and recursive.
 * @author K. Benedyczak
 */
public class GroupsSelectionList extends TwinColSelect
{
	private Collection<String> groups;
	private GroupsManagement groupsMan;
	private List<String> processedGroups = new ArrayList<>();

	public GroupsSelectionList(String caption, Collection<String> groups)
	{
		super(caption);
		this.groups = groups;
		initContent();
	}

	public GroupsSelectionList(String caption, GroupsManagement groupsMan)
	{
		super(caption);
		this.groupsMan = groupsMan;
		initContent();
	}
	
	@SuppressWarnings("unchecked")
	public Collection<String> getSelectedGroups()
	{
		return (Collection<String>) getValue();
	}
	
	public void setSelectedGroups(Collection<String> groups)
	{
		setValue(groups);
	}

	public List<String> getAllGroups()
	{
		return new ArrayList<>(processedGroups);
	}

	public void setInput(String rootGroup, boolean inclusive)
	{
		removeAllItems();
		processedGroups = GroupSelectionUtils.establishGroups(
				rootGroup, inclusive, groupsMan, groups);
		for (String group: processedGroups)
			addItem(group);
	}

	private void initContent()
	{
		setRows(5);
	}
}
