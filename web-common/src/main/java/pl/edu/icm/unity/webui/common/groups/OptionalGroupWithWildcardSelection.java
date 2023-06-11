/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webui.common.groups;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.util.StringUtils;

import com.vaadin.event.selection.SingleSelectionEvent;
import com.vaadin.ui.Button.ClickEvent;

import pl.edu.icm.unity.base.group.Group;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.registration.GroupPatternMatcher;
import pl.edu.icm.unity.webui.common.chips.ChipsWithDropdown;
import pl.edu.icm.unity.webui.common.groups.GroupSelectionHelper.GroupNameComparator;

/**
 * {@link ChipsWithDropdown} specialization for selecting multiple groups with
 * wildcards supported. Children of selected groups are not available for
 * selection
 * 
 * @author P.Piernik
 *
 */
public class OptionalGroupWithWildcardSelection extends ChipsWithDropdown<Group> implements GroupsSelection
{
	private MessageSource msg;

	public OptionalGroupWithWildcardSelection(MessageSource msg)
	{
		this(msg, true);
	}

	public OptionalGroupWithWildcardSelection(MessageSource msg, boolean multiSelectable)
	{
		super(group -> group.getDisplayedName().getValue(msg), group -> group.getDisplayedName().getValue(msg), true);
		this.msg = msg;
		if (!multiSelectable)
		{
			setMaxSelection(1);
			addChipRemovalListener(this::onSingleGroupRemoval);
		}

		addSelectionListener(this::onGroupSelection);

		combo.setNewItemProvider(s ->
		{
			if (getValue().contains(new Group(s)))
				return Optional.empty();
			return Optional.of(new Group(s));
		});
		combo.setDescription(msg.getMessage("typeOrSelect"));
		combo.setPlaceholder(msg.getMessage("typeOrSelect"));
	}

	@Override
	public Set<String> getItems()
	{
		return super.getAllItems().stream().map(g -> g.toString()).collect(Collectors.toSet());
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
		Set<Group> newGroups = evalSelected(List.of(event.getValue()));
		List<Group> selected = new ArrayList<>(getSelectedItems());
		for (Group g : evalSelected(getSelectedItems()))
		{
			if (!g.getPathEncoded().equals(newGroup.getPathEncoded()))
			{
				newGroups.forEach(ng ->
				{
					if (g.isChild(ng))
					{
						selected.remove(g);
					}
				});

			}
		}
		setSelectedItems(selected);
	}

	@Override
	protected List<Group> checkAvailableItems(Set<Group> allItems, Set<Group> selected)
	{
		Set<Group> remaining = super.checkAvailableItems(allItems, evalSelected(selected)).stream()
				.collect(Collectors.toSet());
		Set<Group> ret = new HashSet<>(remaining);

		selected.forEach(selG -> remaining.forEach(remG ->
		{
			if (remG.isChild(selG))
				ret.remove(remG);
		}));

		return ret.stream().collect(Collectors.toList());
	}

	private Set<Group> evalSelected(Collection<Group> selected)
	{
		List<Group> all = getAllItems().stream().collect(Collectors.toList());
		Set<Group> rselected = new HashSet<>();

		selected.forEach(s -> rselected.addAll(GroupPatternMatcher.filterMatching(all, s.getPathEncoded())));
		return rselected;
	}

	@Override
	protected void selectItem(Group selected)
	{
		if (!getSelectedItems().contains(selected))
		{
			super.selectItem(selected);
		}
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
		updateComboRenderer(
				g -> GroupSelectionHelper.generateIndent(StringUtils.countOccurrencesOf(g.toString(), "/") - min)
						+ g.getDisplayedName().getValue(msg));
		super.setItems(items);

	}

	protected void sortItems(List<Group> source)
	{
		GroupSelectionHelper.sort(source, new GroupNameComparator(msg));
	}

	@Override
	public List<String> getSelectedGroupsWithParents()
	{
		return getSelectedGroupsWithoutParents();
	}

	@Override
	public List<String> getSelectedGroupsWithoutParents()
	{
		return getSelectedItems().stream().map(group -> group.toString()).collect(Collectors.toList());
	}

}