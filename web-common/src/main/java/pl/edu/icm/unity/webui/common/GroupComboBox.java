/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.vaadin.v7.ui.ComboBox;

import pl.edu.icm.unity.engine.api.GroupsManagement;


/**
 * Combo box allowing to choose a group. This components can automatically populate the combobox 
 * with subgroups of a given group, both immediate and recursive.
 * @author K. Benedyczak
 * 
 * @deprecated use {@link GroupComboBox2}
 */
@Deprecated
public class GroupComboBox extends ComboBox
{
	private Collection<String> groups;
	private GroupsManagement groupsMan;
	private List<String> processedGroups = new ArrayList<>();

	public GroupComboBox(String caption, Collection<String> groups)
	{
		super(caption);
		this.groups = groups;
		init();
	}

	public GroupComboBox(String caption, GroupsManagement groupsMan)
	{
		super(caption);
		this.groupsMan = groupsMan;
		init();
	}
	
	private void init()
	{
		setNullSelectionAllowed(false);
	}
	
	public List<String> getAllGroups()
	{
		return new ArrayList<>(processedGroups);
	}
	
	public void setInput(String rootGroup, boolean inclusive)
	{
		removeAllItems();
		processedGroups = GroupSelectionUtils.establishGroups(rootGroup, 
				inclusive, groupsMan, groups);
		for (String group: processedGroups)
			addItem(group);
		if (!processedGroups.isEmpty())
			select(processedGroups.get(0));
	}

	@Override
	public String getValue()
	{
		return (String) super.getValue();
	}
}
