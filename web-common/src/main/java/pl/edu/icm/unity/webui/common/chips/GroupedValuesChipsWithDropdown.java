/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webui.common.chips;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Same as {@link ChipsWithDropdown} but elements in dropdown can be grouped. 
 * @author P.Piernik
 *
 */
public class GroupedValuesChipsWithDropdown extends ChipsWithDropdown<String>
{
	public static final String INDENT_CHAR = "\u2003";
	private static final String LABEL_PREFIX = "__label__";
	
	private Map<String, List<String>> groupsWithItems;

	public GroupedValuesChipsWithDropdown(Map<String, List<String>> groupsWithItems)
	{
		this.groupsWithItems = groupsWithItems;
		setComboStyleGenerator(v -> v.startsWith(LABEL_PREFIX) ? "inactive" : "");
		List<String> items = new ArrayList<>();
		groupsWithItems.values().forEach(i -> items.addAll(i));
		setItems(items);
	}

	@Override
	public void setItems(Collection<String> items)
	{
		updateComboRenderer(v -> !v.startsWith(LABEL_PREFIX) ? "\u2003" + v
				: v.substring(LABEL_PREFIX.length(), v.length()));
		super.setItems(items);
	}

	private String getLabelKey(String v)
	{
		return v.substring(LABEL_PREFIX.length(), v.length());
	}

	@Override
	protected List<String> checkAvailableItems(Set<String> allItems, Set<String> selected)
	{
		Set<String> remaining = super.checkAvailableItems(allItems, selected).stream()
				.collect(Collectors.toSet());

		for (String remain : remaining.stream().collect(Collectors.toList()))
		{
			if (remain.startsWith(LABEL_PREFIX))
			{
				if (selected.containsAll(groupsWithItems.get(getLabelKey(remain))))
				{
					remaining.remove(remain);
				}
			}
		}

		return remaining.stream().collect(Collectors.toList());
	}

	@Override
	protected void sortItems(List<String> items)
	{
		List<String> realItems = new ArrayList<>();
		for (String key : groupsWithItems.keySet().stream().sorted().collect(Collectors.toList()))
		{
			List<String> labelItems = new ArrayList<>();
			for (String item : items)
			{
				if (groupsWithItems.get(key).contains(item))
				{
					labelItems.add(item);
				}
			}
			if (!labelItems.isEmpty())
			{
				realItems.add(LABEL_PREFIX + key);
				realItems.addAll(labelItems.stream().sorted().collect(Collectors.toList()));
			}
		}

		items.clear();
		items.addAll(realItems);		
	}

	@Override
	protected void selectItem(String selected)
	{
		if (selected != null)
		{
			if (!selected.startsWith(LABEL_PREFIX))
			{
				super.selectItem(selected);
			} else
			{
				String key = getLabelKey(selected);
				List<String> vLabels = groupsWithItems.get(key);
				for (String item : getAllItems())
				{
					if (vLabels.contains(item) && !getSelectedItems().contains(item))
						super.selectItem(item);
				}
			}

		}
	}
}
