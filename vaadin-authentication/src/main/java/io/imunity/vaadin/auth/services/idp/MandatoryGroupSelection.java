/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.auth.services.idp;

import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import io.imunity.vaadin.endpoint.common.forms.groups.GroupsSelection;
import org.springframework.util.StringUtils;
import pl.edu.icm.unity.base.group.Group;
import pl.edu.icm.unity.base.message.MessageSource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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
		setItemLabelGenerator(g -> g.group().getDisplayedName().getValue(msg));
		setRequiredIndicatorVisible(true);
		addValueChangeListener(event -> {
			if(event.isFromClient() && groupChangeConfirmationQuestion != null)
			{
				new ConfirmDialog(
						msg.getMessage("ConfirmDialog.confirm"),
						groupChangeConfirmationQuestion,
						msg.getMessage("ok"),
						e -> {},
						msg.getMessage("cancel"),
						e -> setValue(event.getOldValue())
				).open();
			}
			else if (event.getValue() != null && event.getValue().indent())
				setValue(new GroupWithIndentIndicator(event.getValue().group(), false));
		});
		items = new ArrayList<>();
	}

	@Override
	public List<String> getSelectedGroupsWithParents()
	{
		Group selected = getValue().group();
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
		Group selected = getValue().group();
		return selected == null ? Collections.emptyList() : List.of(selected.toString());
	}
	
	public Group getSelectedGroup()
	{
		return this.getValue() == null ? null : this.getValue().group();
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
		if (items.isEmpty())
			throw new IllegalArgumentException("Can not remove mandatory group selection");
		Group group = items.get(0);
		setValue(new GroupWithIndentIndicator(group, true));
	}

	@Override
	public void setItems(List<Group> items)
	{
		if (items.isEmpty())
			throw new IllegalArgumentException("At least one group is required as a choice");
		final int min = GroupSelectionHelper.getMinIndent(items);
		setItemLabelGenerator(g -> g.indent()
				? GroupSelectionHelper.generateIndent(
						StringUtils.countOccurrencesOf(g.group().toString(), "/") - min)
						+ g.group().getDisplayedName().getValue(msg)
				: g.group().getDisplayedName().getValue(msg));
		this.items.clear();
		this.items.addAll(items);
		GroupSelectionHelper.sort(this.items, new GroupSelectionHelper.GroupNameComparator(msg));
		setItems(this.items.stream().map(g -> new GroupWithIndentIndicator(g, true)).toList());
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
		return items.stream().map(Group::toString).collect(Collectors.toSet());
	}

	
	public void setGroupChangeConfirmationQuestion(String groupChangeConfirmationQuestion)
	{
		this.groupChangeConfirmationQuestion = groupChangeConfirmationQuestion;
	}

	@Override
	public void setReadOnly(boolean readOnly)
	{
		getElement().setProperty("readonly", readOnly);
	}
}
