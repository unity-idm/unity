/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.groups;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.util.StringUtils;

import com.google.common.collect.Lists;
import com.vaadin.ui.ComboBox;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.common.groups.GroupSelectionHelper.GroupNameComparator;

/**
 * Allows to select a single group out of multiple, with assumption that a choice is mandatory.
 * In this case a simple dropdown is used instead of chips component.
 * 
 * @author K. Benedyczak
 */
public class MandatoryGroupSelection extends ComboBox<GroupWithIndentIndicator> implements GroupsSelection
{
	private UnityMessageSource msg;
	private List<Group> items;
	
	public MandatoryGroupSelection(UnityMessageSource msg)
	{
		this.msg = msg;
		setItemCaptionGenerator(g -> g.group.getDisplayedName().getValue(msg));
		setEmptySelectionAllowed(false);
		setRequiredIndicatorVisible(true);
		setStyleName(Styles.indentComboBox.toString());
		addValueChangeListener(e -> {
			if (e.getValue().indent)
				setValue(new GroupWithIndentIndicator(e.getValue().group, false));
		});
		items = new ArrayList<>();
	}

	@Override
	public List<String> getSelectedGroups()
	{
		Group selected = getValue().group;
		if (selected == null)
			return Lists.newArrayList();

		List<Group> realSelected = new ArrayList<>();
		realSelected.add(selected);
		for (Group g : items)
		{
			if (selected.isChild(g) && !realSelected.contains(g))
			{
				realSelected.add(g);
			}
		}
		return realSelected.stream().map(group -> group.toString()).collect(Collectors.toList());
	}
	
	public String getSelectedGroup()
	{
		return this.getValue() == null ? null : this.getValue().group.toString();
	}
	
	@Override
	public void setSelectedItems(List<Group> items)
	{
		if (items.size() > 1)
			throw new IllegalArgumentException("Can not select more then one element in single-selectable group selection");
		if (items.isEmpty())
			throw new IllegalArgumentException("Can not remove mandatory group selection");
		Group group = items.get(0);
		setSelectedItem(new GroupWithIndentIndicator(group, true));
	}

	@Override
	public void setItems(List<Group> items)
	{
		if (items.isEmpty())
			throw new IllegalArgumentException("At least one group is required as a choice");
		final int min = GroupSelectionHelper.getMinIndent(items);
		setItemCaptionGenerator(g -> g.indent
				? GroupSelectionHelper.generateIndent(
						StringUtils.countOccurrencesOf(g.group.toString(), "/") - min)
						+ g.group.getDisplayedName().getValue(msg)
				: g.group.getDisplayedName().getValue(msg));
		GroupSelectionHelper.sort(items, new GroupNameComparator(msg));
		this.items.clear();
		this.items.addAll(items);
		super.setItems(this.items.stream().map(g -> new GroupWithIndentIndicator(g, true)));
		setSelectedItem(new GroupWithIndentIndicator(this.items.get(0), true));
	}

	@Override
	public void setMultiSelectable(boolean multiSelect)
	{
		if (multiSelect)
			throw new IllegalStateException("Can not change single selected mandatory component to multiselect component.");
	}
	
	@Override
	public Set<String> getItems()
	{
		return items.stream().map(g -> g.toString()).collect(Collectors.toSet());
	}
}
