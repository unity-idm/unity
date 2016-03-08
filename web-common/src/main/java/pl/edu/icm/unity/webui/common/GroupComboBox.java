/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common;

import java.util.Collection;

import pl.edu.icm.unity.server.api.GroupsManagement;

import com.vaadin.ui.ComboBox;


/**
 * Combo box allowing to choose a group. This components can automatically populate the combobox 
 * with subgroups of a given group, both immediate and recursive.
 * @author K. Benedyczak
 */
public class GroupComboBox extends GroupSelectionComponent
{
	private ComboBox contents;
	
	public GroupComboBox(String caption, Collection<String> groups)
	{
		super(caption, groups);
		init();
	}

	public GroupComboBox(String caption, GroupsManagement groupsMan)
	{
		super(caption, groupsMan);
		init();
	}
	
	private void init()
	{
		contents = new ComboBox();
		contents.setNullSelectionAllowed(false);
		setCompositionRoot(contents);
	}

	public void setInput(String rootGroup, boolean inclusive)
	{
		contents.removeAllItems();
		super.setInput(rootGroup, inclusive);
		for (String group: fixedGroups)
			contents.addItem(group);
		if (!fixedGroups.isEmpty())
			contents.select(fixedGroups.get(0));
	}

	public void setValue(String group)
	{
		contents.setValue(group);
	}

	public String getValue()
	{
		return (String) contents.getValue();
	}

	public void setNullSelectionAllowed(boolean b)
	{
		contents.setNullSelectionAllowed(b);		
	}
}
