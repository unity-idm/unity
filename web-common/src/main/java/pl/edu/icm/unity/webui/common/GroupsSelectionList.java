/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common;

import java.util.Collection;

import pl.edu.icm.unity.server.api.GroupsManagement;

import com.vaadin.ui.ListSelect;


/**
 * {@link ListSelect} allowing to choose a set of groups. This component can automatically populate the list 
 * with subgroups of a given group, both immediate and recursive.
 * @author K. Benedyczak
 */
public class GroupsSelectionList extends GroupSelectionComponent
{
	private ListSelect contents;
	
	public GroupsSelectionList(String caption, Collection<String> groups)
	{
		super(caption, groups);
		init();
	}

	public GroupsSelectionList(String caption, GroupsManagement groupsMan)
	{
		super(caption, groupsMan);
		init();
	}
	
	private void init()
	{
		contents = new ListSelect();
		contents.setMultiSelect(true);
		setCompositionRoot(contents);
	}
	
	@SuppressWarnings("unchecked")
	public Collection<String> getSelectedGroups()
	{
		return (Collection<String>) contents.getValue();
	}
	
	public void setSelectedGroups(Collection<String> groups)
	{
		contents.setValue(groups);
	}
	
	public void setInput(String rootGroup, boolean inclusive)
	{
		contents.removeAllItems();
		super.setInput(rootGroup, inclusive);
		for (String group: fixedGroups)
			contents.addItem(group);
	}
}
