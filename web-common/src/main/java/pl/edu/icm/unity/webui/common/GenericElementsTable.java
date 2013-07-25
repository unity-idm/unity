/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common;

import java.util.Collection;

import com.vaadin.data.util.BeanItemContainer;
import com.vaadin.ui.Table;

/**
 * 1-column table with arbitrary objects. 
 * Allows for sorting, disable multiselect, uses {@link BeanItemContainer}.
 * The value is obtaned either via toString() method of the content item or via a given implementation 
 * of {@link NameProvider}.
 * @author K. Benedyczak
 */
public class GenericElementsTable<T> extends Table
{
	private NameProvider<T> nameProvider;
	
	public GenericElementsTable(String columnHeader, Class<T> clazz)
	{
		this(columnHeader, clazz, new DefaultNameProvider<T>());
	}
	
	public GenericElementsTable(String columnHeader, Class<T> clazz, NameProvider<T> nameProvider)
	{
		this.nameProvider = nameProvider;
		setNullSelectionAllowed(false);
		setImmediate(true);
		setSizeFull();
		BeanItemContainer<GenericItem<T>> tableContainer = new BeanItemContainer<GenericItem<T>>(
				GenericItem.class);
		tableContainer.removeContainerProperty("element");
		setSelectable(true);
		setMultiSelect(false);
		setContainerDataSource(tableContainer);
		setColumnHeaders(new String[] {columnHeader});
		setSortContainerPropertyId(getContainerPropertyIds().iterator().next());
		setSortAscending(true);
	}
	
	public void setInput(Collection<T> types)
	{
		removeAllItems();
		for (T attributeType: types)
			addItem(new GenericItem<T>(attributeType, nameProvider));
		sort();
	}
	
	public void addElement(T el)
	{
		addItem(new GenericItem<T>(el, nameProvider));
		sort();
	}
	
	public interface NameProvider<T>
	{
		public String toString(T element);
	}
	
	public static class GenericItem<T>
	{
		private T element;
		private NameProvider<T> nameProvider;

		public GenericItem(T value, NameProvider<T> nameProvider)
		{
			this.element = value;
			this.nameProvider = nameProvider;
		}
		
		public String getName()
		{
			return nameProvider.toString(element); 
		}
		
		public T getElement()
		{
			return element;
		}
	}
	
	private static class DefaultNameProvider<T> implements NameProvider<T>
	{
		@Override
		public String toString(T element)
		{
			return element.toString();
		}
	}
}
