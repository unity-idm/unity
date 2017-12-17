/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import com.vaadin.data.ValueProvider;
import com.vaadin.data.provider.DataProvider;
import com.vaadin.data.provider.ListDataProvider;

/**
 * 1-column table with arbitrary objects. 
 * Allows for sorting and by default disables multiselect.
 * The value is obtained either via toString() method of the content item or via a 
 * given implementation.
 * 
 * @author K. Benedyczak
 */
public class GenericElementsTable2<T> extends SmallGrid<T>
{
	private List<T> contents;
	private ListDataProvider<T> dataProvider;
	private Column<T, String> col1;
	private GridContextMenuSupport<T> contextMenuSupp;

	
	public GenericElementsTable2(String columnHeader)
	{
		this(columnHeader, new DefaultNameProvider<T>());
	}
	
	public GenericElementsTable2(String columnHeader, ValueProvider<T, String> nameProvider)
	{
		contents = new ArrayList<>();
		dataProvider = DataProvider.ofCollection(contents);
		setDataProvider(dataProvider);
		setSizeFull();
		setSelectionMode(SelectionMode.SINGLE);
		col1 = addColumn(nameProvider, n -> n)
				.setCaption(columnHeader)
				.setResizable(false);
		GridSelectionSupport.installClickListener(this);
		sort(col1);
		contextMenuSupp = new GridContextMenuSupport<>(this);
	}
	
	public void setMultiSelect(boolean multi)
	{
		setSelectionMode(multi ? SelectionMode.MULTI : SelectionMode.SINGLE);
	}
	
	public void addActionHandler(SingleActionHandler2<T> actionHandler) 
	{
		contextMenuSupp.addActionHandler(actionHandler);
	}

	public List<SingleActionHandler2<T>> getActionHandlers()
	{
		return contextMenuSupp.getActionHandlers();
	}
	
	public void setInput(Collection<? extends T> elements)
	{
		Set<T> selectedItems = getSelectedItems();
		contents.clear();
		contents.addAll(elements);
		dataProvider.refreshAll();
		sort(col1);
		deselectAll();
		for (T toSelect : selectedItems)
		{
			if (elements.contains(toSelect))
			{
				select(toSelect);
			}
		}
	}
	
	public void addElement(T el)
	{
		contents.add(el);
		dataProvider.refreshItem(el);
		sort(col1);
	}

	public void removeElement(T el)
	{
		contents.remove(el);
		dataProvider.refreshAll();
		sort(col1);
	}
	
	public List<T> getElements()
	{
		return new ArrayList<>(contents);
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
