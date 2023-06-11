/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.endpoint.common.forms.groups;

import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.data.renderer.ComponentRenderer;

import pl.edu.icm.unity.base.message.MessageSource;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class GroupMultiComboBox extends MultiSelectComboBox<GroupTreeNode>
{
	private final MessageSource msg;

	public GroupMultiComboBox(MessageSource msg)
	{
		this.msg = msg;
		setRenderer(new ComponentRenderer<>(this::renderGroupWithIndent));
		addValueChangeListener(this::blockNullValue);
		setItemLabelGenerator(event -> event.group.getDisplayedName().getValue(msg));

		addValueChangeListener(event ->
		{
			if(!event.isFromClient())
				return;

			Set<GroupTreeNode> selectedGroups = new HashSet<>(event.getValue());
			Set<GroupTreeNode> lastSelectedGroup = new HashSet<>(event.getOldValue());

			addAllGroupsAncestorIfNewGroupAdded(selectedGroups, lastSelectedGroup);
			removeAllOffspringsIfParentWasRemoved(selectedGroups, lastSelectedGroup);

			setValue(selectedGroups);
		});
	}

	public List<String> getSelectedGroupsWithoutParents()
	{
		List<String> paths = new ArrayList<>();
		Set<GroupTreeNode> values = getValue();
		for (GroupTreeNode value : values)
		{
			boolean match = values.stream()
					.filter(node -> !node.equals(value))
					.anyMatch(node -> value.parent.map(parent -> parent.equals(node)).orElse(false));
			if(match)
			{
				paths.add(value.getPath());
			}
		}
		return paths;
	}

	private void removeAllOffspringsIfParentWasRemoved(Set<GroupTreeNode> newSet, Set<GroupTreeNode> oldSet)
	{
		HashSet<GroupTreeNode> nodes = new HashSet<>(oldSet);
		nodes.removeAll(newSet);
		nodes.stream()
				.map(GroupTreeNode::getNodeWithAllOffspring)
				.forEach(newSet::removeAll);
	}

	private void addAllGroupsAncestorIfNewGroupAdded(Set<GroupTreeNode> newSet, Set<GroupTreeNode> oldSet)
	{
		HashSet<GroupTreeNode> nodes = new HashSet<>(newSet);
		nodes.removeAll(oldSet);
		nodes.stream()
				.map(GroupTreeNode::getAllAncestors)
				.forEach(newSet::addAll);
	}

	private void blockNullValue(ComponentValueChangeEvent<MultiSelectComboBox<GroupTreeNode>, Set<GroupTreeNode>> event)
	{
		if(event.getValue() == null && event.isFromClient())
			setValue(event.getOldValue());
	}

	private Div renderGroupWithIndent(GroupTreeNode group)
	{
		Div div = new Div(new Text(group.getDisplayedName(msg)));
		div.getStyle().set("text-indent", group.getLevel() + "em");
		return div;
	}
}
