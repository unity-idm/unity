/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.groups;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.util.StringUtils;

import com.vaadin.event.selection.SingleSelectionEvent;
import com.vaadin.ui.Button.ClickEvent;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.webui.common.chips.ChipsWithDropdown;
import pl.edu.icm.unity.webui.common.groups.GroupSelectionHelper.GroupNameComparator;

/**
 * {@link ChipsWithDropdown} specialization for selecting multiple groups
 * @author K. Benedyczak
 */
public class OptionalGroupsSelection extends ChipsWithDropdown<Group> implements GroupsSelection
{
	private UnityMessageSource msg;
	
	public OptionalGroupsSelection(UnityMessageSource msg)
	{
		this(msg, true);
	}
	
	public OptionalGroupsSelection(UnityMessageSource msg, boolean multiSelectable)
	{
		super(group -> group.getDisplayedName().getValue(msg), group -> group.getDisplayedName().getValue(msg), true);	
		this.msg = msg;
		if (multiSelectable)
		{
			
			addChipRemovalListener(this::onMultiGroupRemoval);
		}else
		{
			setMaxSelection(1);
			addChipRemovalListener(this::onSingleGroupRemoval);
		}
		
		addSelectionListener(this::onGroupSelection);
	}
	
	@Override
	public List<String> getSelectedGroups()
	{
		return getSelectedItems().stream().map(group -> group.toString()).collect(Collectors.toList());
	}
	
	private void onMultiGroupRemoval(ClickEvent event)
	{
		List<Group> selected = getSelectedItems();
		List<Group> selectedCopy = new ArrayList<>(selected);
		Group g = (Group) event.getButton().getData();

		for (Group s : selectedCopy)
		{
			if (s.isChild(g))
			{
				selected.remove(s);
			}
		}
		setSelectedItems(selected);
	}
	
	private void onSingleGroupRemoval(ClickEvent event)
	{
		setSelectedItems(Collections.emptyList());
	}

	private void onGroupSelection(SingleSelectionEvent<Group> event)
	{
		Group newGroup = event.getValue();
		if (newGroup == null)
			return;

		List<Group> selected = new ArrayList<>(getSelectedItems());
		Set<Group> allItems = getItems();

		for (Group g : allItems)
		{
			if (newGroup.isChild(g) && !selected.contains(g))
			{
				selected.add(g);
			}
		}
		setSelectedItems(selected);
	}
	
	@Override
	public void setSelectedItems(List<Group> items)
	{
		GroupSelectionHelper.sort(items, new GroupNameComparator(msg));
		super.setSelectedItems(items);
	}
	
	@Override
	public void setItems(List<Group> items)
	{
		final int min = GroupSelectionHelper.getMinIndent(items);
		updateComboRenderer(g -> GroupSelectionHelper.generateIndent(StringUtils.countOccurrencesOf(g.toString(), "/") - min)
				+ g.getDisplayedName().getValue(msg));	
		super.setItems(items);
		
	}

	protected void sortItems(List<Group> source)
	{
		GroupSelectionHelper.sort(source, new GroupNameComparator(msg));
	}
}

