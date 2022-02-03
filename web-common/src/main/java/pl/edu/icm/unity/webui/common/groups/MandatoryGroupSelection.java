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

import com.google.common.collect.ImmutableList;
import com.vaadin.ui.ComboBox;

import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.webui.common.ConfirmDialog;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.common.groups.GroupSelectionHelper.GroupNameComparator;

/**
 * Allows to select a single group out of multiple, with assumption that a
 * choice is mandatory. In this case a simple dropdown is used instead of chips
 * component.
 * 
 * @author K. Benedyczak
 */
public class MandatoryGroupSelection extends ComboBox<GroupWithIndentIndicator> implements GroupsSelection
{
	private MessageSource msg;
	private List<Group> items;
	private String groupChangeConfirmationQuestion;
	
	public MandatoryGroupSelection(MessageSource msg)
	{
		this.msg = msg;
		setItemCaptionGenerator(g -> g.group.getDisplayedName().getValue(msg));
		setEmptySelectionAllowed(false);
		setRequiredIndicatorVisible(true);
		setStyleName(Styles.indentComboBox.toString());
		addValueChangeListener(e -> {
			if (e.getValue() != null && e.getValue().indent)
				setValue(new GroupWithIndentIndicator(e.getValue().group, false));
		});
		items = new ArrayList<>();
	}

	@Override
	public List<String> getSelectedGroupsWithParents()
	{
		Group selected = getValue().group;
		if (selected == null)
			return Collections.emptyList();

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

	@Override
	public List<String> getSelectedGroupsWithoutParents()
	{
		Group selected = getValue().group;
		return selected == null ? Collections.emptyList() : ImmutableList.of(selected.toString());
	}
	
	public Group getSelectedGroup()
	{
		return this.getValue() == null ? null : this.getValue().group;
	}

	public void refreshCaptions()
	{
		GroupWithIndentIndicator selected = getSelectedItem().orElse(null);
		if (selected != null)
		{
			setSelectedItem(new GroupWithIndentIndicator(new Group("/"), false));
			setSelectedItem(selected);
		}
	}

	@Override
	public void setSelectedItems(List<Group> items)
	{
		if (items.size() > 1)
			throw new IllegalArgumentException(
					"Can not select more then one element in single-selectable group selection");
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
		this.items.clear();
		this.items.addAll(items);
		GroupSelectionHelper.sort(this.items, new GroupNameComparator(msg));
		super.setItems(this.items.stream().map(g -> new GroupWithIndentIndicator(g, true)));
		setSelectedItem(new GroupWithIndentIndicator(this.items.get(0), true));
	}

	@Override
	public void setMultiSelectable(boolean multiSelect)
	{
		if (multiSelect)
			throw new IllegalStateException(
					"Can not change single selected mandatory component to multiselect component.");
	}

	@Override
	public Set<String> getItems()
	{
		return items.stream().map(g -> g.toString()).collect(Collectors.toSet());
	}

	@Override
	protected void setSelectedItem(GroupWithIndentIndicator value, boolean userOriginated)
	{
		
		if (userOriginated && groupChangeConfirmationQuestion != null)
		{
			new ConfirmDialog(msg, groupChangeConfirmationQuestion, () -> super.setSelectedItem(value, userOriginated)).show();
		}else
		{
			super.setSelectedItem(value, userOriginated);
		}
	}
	
	public void setGroupChangeConfirmationQuestion(String groupChangeConfirmationQuestion)
	{
		this.groupChangeConfirmationQuestion = groupChangeConfirmationQuestion;
	}	
}
