/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import com.vaadin.data.ValueProvider;
import com.vaadin.data.provider.DataProvider;
import com.vaadin.data.provider.GridSortOrder;
import com.vaadin.data.provider.ListDataProvider;
import com.vaadin.data.provider.Query;
import com.vaadin.server.SerializablePredicate;
import com.vaadin.shared.data.sort.SortDirection;

/**
 * 1-column table with arbitrary objects. 
 * Allows for sorting and by default disables multiselect.
 * The value is obtained either via toString() method of the content item or via a 
 * given implementation.
 * 
 * @author K. Benedyczak
 */
public class GenericElementsTable<T> extends SmallGrid<T>
{
	protected List<T> contents;
	private ListDataProvider<T> dataProvider;
	private Column<T, String> col1;
	private GridContextMenuSupport<T> contextMenuSupp;
	private boolean sortable;
	private Collection<SerializablePredicate<T>> filters;
	private ValueProvider<T, String> nameProvider;
	
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
		this.nameProvider = nameProvider;
		contents = new ArrayList<>();
		dataProvider = DataProvider.ofCollection(contents);
		setDataProvider(dataProvider);
		setSizeFull();
		setSelectionMode(SelectionMode.SINGLE);
		col1 = addColumn(nameProvider, n -> n)
				.setCaption(columnHeader)
				.setResizable(false);
		GridSelectionSupport.installClickListener(this);
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

	public List<SingleActionHandler<T>> getActionHandlers()
	{
		return contextMenuSupp.getActionHandlers();
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
	
	public void selectFirst()
	{
		Optional<T> first;
		Stream<T> fetchStream = dataProvider.fetch(new Query<>());

		if (!sortable)
		{
			first = fetchStream.findFirst();

		} else
		{
			List<GridSortOrder<T>> order = getSortOrder();
			SortDirection dir = order.get(0).getDirection();

			if (dir.equals(SortDirection.ASCENDING))

				first = fetchStream.sorted(Comparator.comparing(nameProvider))
						.findFirst();
			else

				first = fetchStream.sorted(
						Comparator.comparing(nameProvider).reversed())
						.findFirst();
		}
		if (first.isPresent())
			select(first.get());
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
			sort(col1);
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
