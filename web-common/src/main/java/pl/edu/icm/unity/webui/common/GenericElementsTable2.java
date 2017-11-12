/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import com.vaadin.data.provider.DataProvider;
import com.vaadin.data.provider.ListDataProvider;
import com.vaadin.event.Action;
import com.vaadin.ui.Label;
import com.vaadin.ui.components.grid.SingleSelectionModel;
import com.vaadin.ui.renderers.ComponentRenderer;

/**
 * 1-column table with arbitrary objects. 
 * Allows for sorting and default disable multiselect, uses {@link BeanItemContainer}.
 * The value is obtained either via toString() method of the content item or via a given implementation 
 * of {@link NameProvider}.
 * @author K. Benedyczak
 */
public class GenericElementsTable2<T> extends SmallTable<T>
{
	private NameProvider<T> nameProvider;
	private List<SingleActionHandler> actionHandlers;
	private List<T> contents;
	private ListDataProvider<T> dataProvider;
	private Column<T, T> col1;

	
	public GenericElementsTable2(String columnHeader)
	{
		this(columnHeader, new DefaultNameProvider<T>());
	}
	
	public GenericElementsTable2(String columnHeader, NameProvider<T> nameProvider)
	{
		this.nameProvider = nameProvider;
		this.actionHandlers = new ArrayList<>();
		contents = new ArrayList<>();
		dataProvider = DataProvider.ofCollection(contents);
		setDataProvider(dataProvider);
		setSizeFull();
		setSelectionMode(SelectionMode.SINGLE);
		SingleSelectionModel<T> selectionModel = (SingleSelectionModel<T>) getSelectionModel();
		selectionModel.setDeselectAllowed(false);
		col1 = addColumn(v -> v, this::getLabelForValue, 
				new ComponentRenderer()).setCaption(columnHeader);
		sort(col1.getId());
	}
	
	public void addActionHandler(Action.Handler actionHandler) {
//TODO
		if (actionHandler instanceof SingleActionHandler)
			actionHandlers.add((SingleActionHandler) actionHandler);
	}

	public List<SingleActionHandler> getActionHandlers()
	{
		return actionHandlers;
	}
	
	public void setInput(Collection<? extends T> elements)
	{
		Set<T> selectedItems = getSelectedItems();
		contents.clear();
		contents.addAll(elements);
		dataProvider.refreshAll();
		sort(col1.getId());
		for (T toSelect: selectedItems)
			select(toSelect);
	}
	
	public void addElement(T el)
	{
		contents.add(el);
		dataProvider.refreshItem(el);
		sort(col1.getId());
	}
	
	private Label getLabelForValue(T value)
	{
		Object representation = nameProvider.toRepresentation(value);
		if (representation instanceof Label)
			return (Label) representation;
		return new Label(representation.toString());
	}
	
	public interface NameProvider<T>
	{
		/**
		 * @param element
		 * @return object of {@link Label} type or any other. In the latter case to toString method will be called 
		 * on the returned object, and the result will be wrapped as {@link Label}.
		 */
		public Object toRepresentation(T element);
	}

	private static class DefaultNameProvider<T> implements NameProvider<T>
	{
		@Override
		public String toRepresentation(T element)
		{
			return element.toString();
		}
	}
}
