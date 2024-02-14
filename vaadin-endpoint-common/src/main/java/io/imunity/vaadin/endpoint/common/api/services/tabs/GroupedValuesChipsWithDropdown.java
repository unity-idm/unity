/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.endpoint.common.api.services.tabs;

import com.vaadin.flow.component.combobox.MultiSelectComboBox;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class GroupedValuesChipsWithDropdown extends MultiSelectComboBox<String>
{
	private static final String LABEL_PREFIX = "__label__";

	public GroupedValuesChipsWithDropdown(Map<String, List<String>> labels)
	{
		setItemLabelGenerator(
				v -> !v.startsWith(LABEL_PREFIX) ? "\u2003" + v : v.substring(LABEL_PREFIX.length(), v.length()));

		List<String> items = new ArrayList<>();
		for (String key : labels.keySet())
		{
			items.add(LABEL_PREFIX + key);
			items.addAll(labels.get(key)
					.stream()
					.sorted()
					.collect(Collectors.toList()));
		}

		setItems(items);
		addSelectionListener(e ->
		{
			if (!e.isFromClient())
				return;

			e.getAddedSelection()
					.stream()
					.filter(s -> s.startsWith(LABEL_PREFIX))
					.forEach(s ->
					{
						select(labels.get(s.substring(LABEL_PREFIX.length(), s.length())));
						deselect(s);
					});
			e.getRemovedSelection()
					.stream()
					.filter(s -> s.startsWith(LABEL_PREFIX))
					.forEach(s -> deselect(labels.get(s.substring(LABEL_PREFIX.length(), s.length()))));
		});
		setAutoExpand(AutoExpandMode.BOTH);
	}

	public Set<String> getSelectedValue()
	{
		return super.getValue().stream()
				.filter(v -> !v.startsWith(LABEL_PREFIX))
				.collect(Collectors.toSet());
	}

}
