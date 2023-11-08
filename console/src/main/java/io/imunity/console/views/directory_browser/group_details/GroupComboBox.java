/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.console.views.directory_browser.group_details;

import com.vaadin.flow.component.combobox.ComboBox;
import io.imunity.console.views.directory_browser.ComboBoxNonEmptyValueSupport;
import pl.edu.icm.unity.engine.api.GroupsManagement;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class GroupComboBox extends ComboBox<String>
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

	private void init()
	{
		ComboBoxNonEmptyValueSupport.install(this);
	}

	@Override
	public String getValue()
	{
		return super.getValue();
	}
}
