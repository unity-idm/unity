/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.scim.console;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.util.StringUtils;

import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.combobox.dataview.ComboBoxListDataView;
import com.vaadin.flow.data.selection.MultiSelectionEvent;

import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonObject;
import pl.edu.icm.unity.base.group.Group;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.registration.GroupPatternMatcher;
import io.imunity.vaadin.auth.services.idp.GroupSelectionHelper;
import io.imunity.vaadin.auth.services.idp.GroupSelectionHelper.GroupNameComparator;

class OptionalGroupWithWildcardSelection extends MultiSelectComboBox<Group>
{
	private Set<Group> items;
	private MessageSource msg;

	OptionalGroupWithWildcardSelection(MessageSource msg)
	{
		this.msg = msg;
		items = new HashSet<>();

		setAllowCustomValue(true);
		addCustomValueSetListener(event ->
		{
			HashSet<Group> values = new HashSet<>(getValue());
			values.add(new Group(event.getDetail()));
			setValue(values);
		});
		addSelectionListener(e ->
		{
			if (!e.isFromClient())
			{
				refresh();
				return;
			}
			if (e.getAddedSelection() != null && !e.getAddedSelection()
					.isEmpty())
			{
				onAddedGroupSelection(e);
			}
			refresh();
		});
	}

	private void refresh()
	{
		refreshValue();
		List<Group> setItems = checkAvailableItems(items, getValue());
		GroupSelectionHelper.sort(setItems, new GroupSelectionHelper.GroupNameComparator(msg));
		final int min = GroupSelectionHelper.getMinIndent(setItems);

		super.setItems(setItems);
		setItemLabelGenerator(
				g -> GroupSelectionHelper.generateIndent(StringUtils.countOccurrencesOf(g.toString(), "/") - min)
						+ g.getDisplayedName()
								.getValue(msg));
	}

	private void onAddedGroupSelection(MultiSelectionEvent<MultiSelectComboBox<Group>, Group> event)
	{
		Group newGroup = event.getAddedSelection()
				.iterator()
				.next();
		if (newGroup == null)
			return;
		Set<Group> newGroups = evalSelected(event.getAddedSelection());
		List<Group> selected = new ArrayList<>(getSelectedItems());
		for (Group g : evalSelected(getSelectedItems()))
		{
			if (!g.getPathEncoded()
					.equals(newGroup.getPathEncoded()))
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
		setValue(selected.stream()
				.collect(Collectors.toSet()));
	}

	protected List<Group> checkAvailableItems(Set<Group> allItems, Set<Group> selected)
	{
		Set<Group> remaining = allItems.stream()
				.filter(i -> !selected.contains(i))
				.collect(Collectors.toSet());
		Set<Group> ret = new HashSet<>(remaining);

		selected.forEach(selG -> remaining.forEach(remG ->
		{
			if (remG.isChild(selG))
				ret.remove(remG);
		}));

		return ret.stream()
				.collect(Collectors.toList());
	}

	private Set<Group> evalSelected(Collection<Group> selected)
	{
		List<Group> all = items.stream()
				.collect(Collectors.toList());
		Set<Group> rselected = new HashSet<>();

		selected.forEach(s -> rselected.addAll(GroupPatternMatcher.filterMatching(all, s.getPathEncoded())));
		return rselected;
	}

	@Override
	protected void refreshValue()
	{
		Set<Group> value = getValue();
		if (value == null || value.isEmpty())
		{
			return;
		}
		JsonArray selectedItems = modelToPresentation(this, value, msg);
		getElement().setPropertyJson("selectedItems", selectedItems);
	}

	private static JsonArray modelToPresentation(OptionalGroupWithWildcardSelection multiSelectComboBox,
			Set<Group> model, MessageSource msg)
	{
		JsonArray array = Json.createArray();
		if (model == null || model.isEmpty())
		{
			return array;
		}
		List<Group> sortedModel = new ArrayList<Group>(model);
		GroupSelectionHelper.sort(sortedModel, new GroupNameComparator(msg));
		sortedModel.stream()
				.map(g -> multiSelectComboBox.generateJson(g))
				.forEach(jsonObject -> array.set(array.length(), jsonObject));

		return array;
	}

	private JsonObject generateJson(Group item)
	{
		JsonObject jsonObject = Json.createObject();
		jsonObject.put("key", getKeyMapper().key(item));
		getDataGenerator().generateData(item, jsonObject);
		jsonObject.put("label", jsonObject.getString("label")
				.replace(GroupSelectionHelper.GROUPS_TREE_INDENT_CHAR, ""));
		return jsonObject;
	}

	@Override
	public ComboBoxListDataView<Group> setItems(Collection<Group> items)
	{
		List<Group> sorted = items.stream()
				.collect(Collectors.toList());
		GroupSelectionHelper.sort(sorted, new GroupSelectionHelper.GroupNameComparator(msg));
		ComboBoxListDataView<Group> stringComboBoxListDataView = super.setItems(sorted);
		this.items = stringComboBoxListDataView.getItems()
				.collect(Collectors.toSet());
		final int min = GroupSelectionHelper.getMinIndent(sorted);
		setItemLabelGenerator(
				g -> GroupSelectionHelper.generateIndent(StringUtils.countOccurrencesOf(g.toString(), "/") - min)
						+ g.getDisplayedName()
								.getValue(msg));

		return stringComboBoxListDataView;
	}

	@Override
	public void setValue(Set<Group> values)
	{
		if (values == null)
			return;
		items.addAll(values);
		setItems(items);
		super.setValue(values);
	}
}
