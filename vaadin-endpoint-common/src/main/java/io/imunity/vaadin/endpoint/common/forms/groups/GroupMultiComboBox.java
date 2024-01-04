/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.endpoint.common.forms.groups;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.data.renderer.ComponentRenderer;

import io.imunity.vaadin.elements.CssClassNames;
import pl.edu.icm.unity.base.group.Group;
import pl.edu.icm.unity.base.message.MessageSource;

public class GroupMultiComboBox extends MultiSelectComboBox<GroupTreeNode>
{
	private final MessageSource msg;
	private boolean multiselect = true;
	
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
			if (!multiselect)
			{
				if (selectedGroups.size() > 0)
				{
					setOpened(false);
					setAutoOpen(false);
					addClassName(CssClassNames.HIDDEN_COMBO_TOGGLE_BUTTON.getName());
				}else
				{
					setAutoOpen(true);
					removeClassName(CssClassNames.HIDDEN_COMBO_TOGGLE_BUTTON.getName());
				}
			}
			
		});
	}
	
//	GroupTreeNode getSelected(String path)
//	{
//		ListDataProvider<GroupTreeNode> dataProvider = (ListDataProvider<GroupTreeNode>) getDataProvider();
//		return dataProvider.getItems().stream().filter(g -> g.group.getPathEncoded().equals(path)).findFirst().get();
//	}

	public List<String> getSelectedGroupsWithoutParents()
	{
		return Group.getOnlyChildrenOfSet(getValue().stream()
				.map(g -> g.group)
				.collect(Collectors.toSet()))
				.stream()
				.map(Group::getPathEncoded)
				.collect(Collectors.toList());
	}
	
	public List<String> getSelectedGroupsWithParents()
	{
		List<String> paths = new ArrayList<>();
		Set<GroupTreeNode> values = getValue();
		for (GroupTreeNode value : values)
		{
			addParent(paths, value.parent);
			paths.add(value.getPath());
		}
		return paths;
	}
	
	public void setItems(List<Group> allowedFilteredByMode)
	{
		setItems(getGroupTreeNode(allowedFilteredByMode).getAllOffspring());	
	}
	
	public void setMultiSelect(boolean multiselect)
	{
		this.multiselect = multiselect;
	}
	
	private GroupTreeNode getGroupTreeNode(List<Group> allowedFilteredByMode)
	{
		GroupTreeNode groupTreeNode = new GroupTreeNode(new Group("/"), 0, msg);
		allowedFilteredByMode
				.stream()
				.sorted(Comparator.comparing(Group::getPathEncoded))
				.forEach(groupTreeNode::addChild);
		return groupTreeNode;
	}

	private void addParent(List<String> paths, Optional<GroupTreeNode> parent)
	{
		if (parent.isEmpty())
			return;
		if (paths.contains(parent.get().getPath()))
			return;
		paths.add(parent.get().getPath());
		addParent(paths, parent.get().parent);
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
