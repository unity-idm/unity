/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.upman.common;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.vaadin.data.provider.DataProvider;
import com.vaadin.data.provider.ListDataProvider;
import com.vaadin.server.SerializablePredicate;
import com.vaadin.ui.Grid;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.webui.common.GridSelectionSupport;

/**
 * Base for UpMan grids
 * 
 * @author P.Piernik
 */
public abstract class UpManGrid<T> extends Grid<T>
{
	protected final UnityMessageSource msg;
	private List<T> entries;
	private ListDataProvider<T> dataProvider;
	private Function<T, String> idProvider;
	
	public UpManGrid(UnityMessageSource msg, Function<T, String> idProvider)
	{
		this.msg = msg;
		this.idProvider = idProvider;
		entries = new ArrayList<>();
		dataProvider = DataProvider.ofCollection(entries);
		setDataProvider(dataProvider);
		setSelectionMode(SelectionMode.MULTI);
		GridSelectionSupport.installClickListener(this);
		setSizeFull();
	}

	public void setValue(Collection<T> items)
	{
		Set<T> selectedItems = getSelectedItems();
		deselectAll();
		entries.clear();
		entries.addAll(items);
		if (entries.size() <= 18)
			setHeightByRows(entries.isEmpty() ? 1 : entries.size());
		else
			setHeight(100, Unit.PERCENTAGE);
		dataProvider.refreshAll();

		for (String selected : selectedItems.stream().map(s -> idProvider.apply(s))
				.collect(Collectors.toList()))
		{
			for (T entry : entries)
				if (idProvider.apply(entry).equals(selected))
					select(entry);
		}
	}

	protected List<T> getItems()
	{
		return entries;
	}
	
	public void addFilter(SerializablePredicate<T> filter)
	{
		dataProvider.addFilter(filter);
	}

	public void clearFilters()
	{
		dataProvider.clearFilters();
	}
}
