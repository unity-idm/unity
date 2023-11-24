/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.console.views.directory_browser.group_details;

import io.imunity.console.components.NonEmptyComboBox;
import pl.edu.icm.unity.engine.api.GroupsManagement;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class GroupComboBox extends NonEmptyComboBox<String>
{
	private Collection<String> groups;
	private GroupsManagement groupsMan;
	private List<String> processedGroups = new ArrayList<>();

	public GroupComboBox(String caption, Collection<String> groups)
	{
		super(caption);
		this.groups = groups;
	}

	public GroupComboBox(String caption, GroupsManagement groupsMan)
	{
		super(caption);
		this.groupsMan = groupsMan;
	}

	
	public List<String> getAllGroups()
	{
		return new ArrayList<>(processedGroups);
	}
	
	public void setInput(String rootGroup, boolean inclusive)
	{
		processedGroups = GroupSelectionUtils.establishGroups(rootGroup,
				inclusive, groupsMan, groups);
		setItems(processedGroups);
		if (!processedGroups.isEmpty())
			setValue(processedGroups.get(0));
	}

	@Override
	public String getValue()
	{
		return super.getValue();
	}
}
