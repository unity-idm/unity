/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.vaadin.contextmenu.GridContextMenu;
import com.vaadin.data.ValueProvider;
import com.vaadin.data.provider.DataProvider;
import com.vaadin.data.provider.ListDataProvider;
import com.vaadin.ui.Grid;

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
	private List<SingleActionHandler2<T>> actionHandlers;
	private List<T> contents;
	private ListDataProvider<T> dataProvider;
	private Column<T, T> col1;
	private GridContextMenu<T> contextMenu;

	
	public GenericElementsTable2(String columnHeader)
	{
		this(columnHeader, new DefaultNameProvider<T>());
	}
	
	@SuppressWarnings("unchecked")
	public GenericElementsTable2(String columnHeader, ValueProvider<T, String> nameProvider)
	{
		this.actionHandlers = new ArrayList<>();
		contents = new ArrayList<>();
		dataProvider = DataProvider.ofCollection(contents);
		setDataProvider(dataProvider);
		setSizeFull();
		setSelectionMode(SelectionMode.SINGLE);
		col1 = addColumn(v -> v, nameProvider)
				.setCaption(columnHeader)
				.setResizable(false);
		addItemClickListener(this::onMouseClick);
		sort(col1);
		contextMenu = new GridContextMenu<>(this);
		contextMenu.addGridBodyContextMenuListener(e ->
		{
			Set<T> selection = new HashSet<>();
			selection.add((T) e.getItem());
			fillContextMenu(selection);
		});
	}
	
	private void fillContextMenu(Set<T> selection)
	{
		contextMenu.removeItems();
		for (SingleActionHandler2<T> handler: actionHandlers)
		{
			if (handler.isVisible(selection))
			{
				contextMenu.addItem(handler.getCaption(), 
						handler.getIcon(), 
						(mi) -> handler.handle(selection))
					.setEnabled(handler.isEnabled(selection));
			}
		}
	}
	
	public void setMultiSelect(boolean multi)
	{
		setSelectionMode(multi ? SelectionMode.MULTI : SelectionMode.SINGLE);
	}
	
	public void addActionHandler(SingleActionHandler2<T> actionHandler) 
	{
		actionHandlers.add(actionHandler);
	}

	public List<SingleActionHandler2<T>> getActionHandlers()
	{
		return actionHandlers;
	}
	
	public void setInput(Collection<? extends T> elements)
	{
		Set<T> selectedItems = getSelectedItems();
		contents.clear();
		contents.addAll(elements);
		dataProvider.refreshAll();
		sort(col1);
		for (T toSelect: selectedItems)
			select(toSelect);
	}
	
	public void addElement(T el)
	{
		contents.add(el);
		dataProvider.refreshItem(el);
		sort(col1);
	}

	private void onMouseClick(Grid.ItemClick<T> event)
	{
		if (event.getMouseEventDetails().isDoubleClick())
			return;
		T item = event.getItem();
		boolean alreadySelected = getSelectedItems().contains(item);
		deselectAll();
		if (!alreadySelected)
			select(item);
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
