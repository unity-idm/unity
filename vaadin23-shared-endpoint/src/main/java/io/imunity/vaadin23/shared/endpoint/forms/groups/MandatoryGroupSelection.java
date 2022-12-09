/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin23.shared.endpoint.forms.groups;

import com.google.common.collect.ImmutableList;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import org.springframework.util.StringUtils;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.webui.common.groups.GroupSelectionHelper;
import pl.edu.icm.unity.webui.common.groups.GroupWithIndentIndicator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class MandatoryGroupSelection extends ComboBox<GroupWithIndentIndicator> implements GroupsSelection
{
	private MessageSource msg;
	private List<Group> items;
	private String groupChangeConfirmationQuestion;
	
	public MandatoryGroupSelection(MessageSource msg)
	{
		this.msg = msg;
		setItemLabelGenerator(g -> g.group.getDisplayedName().getValue(msg));
		setRequired(true);
		setRequiredIndicatorVisible(true);
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
		GroupWithIndentIndicator selected = getValue();
		if (selected != null)
		{
			setValue(new GroupWithIndentIndicator(new Group("/"), false));
			setValue(selected);
		}
	}

	@Override
	public void setSelectedItems(List<Group> items)
	{
		if (items.size() > 1)
			throw new IllegalArgumentException(
					"Can not select more then one element in single-selectable group selection");
		if (items.size() > 0)
		{
			Group group = items.get(0);
			setValue(new GroupWithIndentIndicator(group, true));
		}
	}

	@Override
	public void setItems(List<Group> items)
	{
		if (items.isEmpty())
			throw new IllegalArgumentException("At least one group is required as a choice");
		final int min = GroupSelectionHelper.getMinIndent(items);
		setItemLabelGenerator(g -> g.indent
				? GroupSelectionHelper.generateIndent(
						StringUtils.countOccurrencesOf(g.group.toString(), "/") - min)
						+ g.group.getDisplayedName().getValue(msg)
				: g.group.getDisplayedName().getValue(msg));
		this.items.clear();
		this.items.addAll(items);
		GroupSelectionHelper.sort(this.items, new GroupSelectionHelper.GroupNameComparator(msg));
		super.setItems(this.items.stream().map(g -> new GroupWithIndentIndicator(g, true)));
		setValue(new GroupWithIndentIndicator(this.items.get(0), true));
	}

	@Override
	public void setDescription(String description)
	{

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

	protected void setSelectedItem(GroupWithIndentIndicator value, boolean userOriginated)
	{
		
		if (userOriginated && groupChangeConfirmationQuestion != null)
		{
			new ConfirmDialog(groupChangeConfirmationQuestion, "", "", (e) -> super.setValue(value)).open();
		}else
		{
			setValue(value);
		}
	}
	
	public void setGroupChangeConfirmationQuestion(String groupChangeConfirmationQuestion)
	{
		this.groupChangeConfirmationQuestion = groupChangeConfirmationQuestion;
	}

	@Override
	public void setReadOnly(boolean readOnly)
	{
		super.setReadOnly(readOnly);
	}
}
