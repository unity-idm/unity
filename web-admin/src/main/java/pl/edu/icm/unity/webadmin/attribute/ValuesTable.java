/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.attribute;

import java.util.ArrayList;
import java.util.List;

import com.vaadin.data.Container;
import com.vaadin.data.Item;
import com.vaadin.data.util.BeanItem;
import com.vaadin.data.util.BeanItemContainer;
import com.vaadin.ui.Component;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.webui.common.SmallTable;
import pl.edu.icm.unity.webui.common.attributes.WebAttributeHandler;
import pl.edu.icm.unity.webui.common.attributes.WebAttributeHandler.RepresentationSize;

/**
 * Table with attribute values.
 * @author K. Benedyczak
 */
public class ValuesTable extends SmallTable
{
	private WebAttributeHandler handler;
	
	public ValuesTable(UnityMessageSource msg)
	{
		setSortEnabled(false);
		setNullSelectionAllowed(false);
		setSelectable(true);
		setMultiSelect(false);
		setSizeFull();
		
		BeanItemContainer<ValueItem> tableContainer = new BeanItemContainer<ValueItem>(ValueItem.class);
		setContainerDataSource(tableContainer);
		setColumnHeaders(new String[] {msg.getMessage("Attribute.values")});
	}
	

	public List<String> getValues()
	{
		@SuppressWarnings("unchecked")
		BeanItemContainer<ValueItem> tableContainer = (BeanItemContainer<ValueItem>) 
				getContainerDataSource();
		List<String> ret = new ArrayList<>(tableContainer.size());
		for (ValueItem item: tableContainer.getItemIds())
			ret.add(item.getAttributeValue());
		return ret;
	}
	
	public void setValues(List<String> values, WebAttributeHandler handler)
	{
		this.handler = handler;
		Container tableContainer = getContainerDataSource();
		tableContainer.removeAllItems();
		for (String val: values)
		{
			ValueItem item = new ValueItem(val);
			tableContainer.addItem(item);
		}
	}
	
	public String getItemById(Object itemId)
	{
		final Item sourceItem = getItem(itemId);
		return ((ValueItem)((BeanItem<?>)sourceItem).getBean()).getAttributeValue();
	}
	
	public void updateItem(Object itemId, String newValue)
	{
		Container.Ordered container = (Container.Ordered)getContainerDataSource();
		Object previous = container.prevItemId(itemId);
		container.removeItem(itemId);
		container.addItemAfter(previous, new ValueItem(newValue));
	}
	
	public void addItem(Object after, String newValue)
	{
		Container.Ordered container = (Container.Ordered)getContainerDataSource();
		if (after == null)
			after = container.lastItemId();
		container.addItemAfter(after, new ValueItem(newValue));
	}
	
	public void moveItemAfter(Object toMoveItemId, Object moveAfterItemId)
	{
		final String value = getItemById(toMoveItemId);
		Container.Ordered container = (Container.Ordered)getContainerDataSource();
		container.removeItem(toMoveItemId);
		container.addItemAfter(moveAfterItemId, new ValuesTable.ValueItem(value));
	}
	
	public void moveBefore(Object toMoveItemId, Object moveBeforeItemId)
	{
		final String value = getItemById(toMoveItemId);
		Container.Ordered container = (Container.Ordered)getContainerDataSource();
		Object previous = container.prevItemId(moveBeforeItemId);
		if (toMoveItemId == previous)
			return;
		
		container.removeItem(toMoveItemId);
		container.addItemAfter(previous, new ValuesTable.ValueItem(value));
	}
	
	public class ValueItem
	{
		private String value;
		private Component contents;

		public ValueItem(String value)
		{
			this.value = value;
			contents = handler.getRepresentation(value, RepresentationSize.LINE);
		}
		
		public Component getContents()
		{
			return contents; 
		}
		
		private String getAttributeValue()
		{
			return value;
		}
	}
}
