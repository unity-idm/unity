/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.endpoint.common.plugins.attributes.components;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridSortOrder;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.function.SerializablePredicate;
import com.vaadin.flow.function.ValueProvider;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public class GenericElementsTable<T> extends Grid<T>
{
	protected List<T> contents;
	private final ListDataProvider<T> dataProvider = new  ListDataProvider<>(List.of());
	private final Column<T> col1;
	private final GridContextMenuSupport<T> contextMenuSupp;
	private final boolean sortable;
	private final Collection<SerializablePredicate<T>> filters;

	public GenericElementsTable(String columnHeader)
	{
		this(columnHeader, new DefaultNameProvider<T>());
	}
	
	public GenericElementsTable(String columnHeader, ValueProvider<T, String> nameProvider)
	{
		this(columnHeader, nameProvider, true);
	}
	
	public GenericElementsTable(String columnHeader, ValueProvider<T, String> nameProvider, boolean sortable)
	{
		this.sortable = sortable;
		contents = new ArrayList<>();
		setItems(contents);
		setSizeFull();
		setSelectionMode(SelectionMode.SINGLE);
		col1 = addColumn(nameProvider)
				.setHeader(columnHeader);
		col1.setSortable(sortable);
		sort();
		contextMenuSupp = new GridContextMenuSupport<>(this);
		filters = new ArrayList<>();
		
	}
	
	public void setMultiSelect(boolean multi)
	{
		setSelectionMode(multi ? SelectionMode.MULTI : SelectionMode.SINGLE);
	}

	public void addActionHandler(SingleActionHandler<T> actionHandler)
	{
		contextMenuSupp.addActionHandler(actionHandler);
	}
	
	public void setInput(Collection<? extends T> elements)
	{
		Set<T> selectedItems = getSelectedItems();
		contents.clear();
		if (elements != null)
			contents.addAll(elements);
		dataProvider.refreshAll();
		sort();
		deselectAll();
		for (T toSelect : selectedItems)
		{
			if (elements != null && elements.contains(toSelect))
			{
				select(toSelect);
			}
		}
	}
	
	public void addElement(T el)
	{
		contents.add(el);
		dataProvider.refreshItem(el);
		sort();
	}

	public void removeElement(T el)
	{
		contents.remove(el);
		dataProvider.refreshAll();
		sort();
	}
	
	private void sort()
	{
		if (sortable)
			sort(GridSortOrder.asc(col1).build());
	}

	public List<T> getElements()
	{
		return new ArrayList<>(contents);
	}
	
	private void updateFilters()
	{
		dataProvider.clearFilters();
		for (SerializablePredicate<T> p : filters)
			dataProvider.addFilter(p);
	}
	
	public void addFilter(SerializablePredicate<T> filter)
	{
		if (!filters.contains(filter))
			filters.add(filter);
		updateFilters();
	}
	
	public void removeFilter(SerializablePredicate<T> filter)
	{
		if (filters.contains(filter))
			filters.remove(filter);
		updateFilters();
	}
	
	public void clearFilters()
	{
		dataProvider.clearFilters();
	}
	
	private static class DefaultNameProvider<T> implements ValueProvider<T, String>
	{
		@Override
		public String apply(T element)
		{
			return element.toString();
		}
	}
}
