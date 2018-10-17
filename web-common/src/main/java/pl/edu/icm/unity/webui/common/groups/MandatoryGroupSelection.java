/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.groups;

import java.util.List;

import com.google.common.collect.Lists;
import com.vaadin.ui.ComboBox;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.types.basic.Group;

/**
 * Allows to select a single group out of multiple, with assumption that a choice is mandatory.
 * In this case a simple dropdown is used instead of chips component.
 * 
 * @author K. Benedyczak
 */
public class MandatoryGroupSelection extends ComboBox<Group> implements GroupsSelection
{
	public MandatoryGroupSelection(UnityMessageSource msg)
	{
		setItemCaptionGenerator(group -> group.getDisplayedName().getValue(msg));
		setEmptySelectionAllowed(false);
		setRequiredIndicatorVisible(true);
	}
	
	@Override
	public List<String> getSelectedGroups()
	{
		return Lists.newArrayList(getValue().toString());
	}
	
	@Override
	public void setSelectedItems(List<Group> items)
	{
		if (items.size() > 1)
			throw new IllegalArgumentException("Can not select more then one element in single-selectable group selection");
		if (items.isEmpty())
			throw new IllegalArgumentException("Can not remove mandatory group selection");
		Group group = items.get(0);
		setSelectedItem(group);
	}

	@Override
	public void setItems(List<Group> items)
	{
		if (items.isEmpty())
			throw new IllegalArgumentException("At least one group is required as a choice");		
		super.setItems(items);
		setSelectedItem(items.get(0));
	}

	@Override
	public void setMultiSelectable(boolean multiSelect)
	{
		if (multiSelect)
			throw new IllegalStateException("Can not change single selected mandatory component to multiselect component.");
	}
}
