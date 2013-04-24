/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.attributetype;

import java.util.List;

import pl.edu.icm.unity.server.api.AttributesManagement;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.basic.AttributeType;

import com.vaadin.data.util.BeanItemContainer;
import com.vaadin.ui.Table;

/**
 * Table with attribute types
 * @author K. Benedyczak
 */
public class AttributeTypesTable extends Table
{
	public AttributeTypesTable(UnityMessageSource msg, AttributesManagement attrManagement)
	{
		setNullSelectionAllowed(false);
		setImmediate(true);
		setSizeFull();
		BeanItemContainer<AttributeTypeItem> tableContainer = new BeanItemContainer<AttributeTypeItem>(
				AttributeTypeItem.class);
		setSelectable(true);
		setMultiSelect(false);
		setContainerDataSource(tableContainer);
		setColumnHeaders(new String[] {msg.getMessage("AttributeTypes.types")});
		setSortContainerPropertyId(getContainerPropertyIds().iterator().next());
		setSortAscending(true);
	}
	
	public void setInput(List<AttributeType> types)
	{
		removeAllItems();
		for (AttributeType attributeType: types)
			addItem(new AttributeTypeItem(attributeType));
		sort();
	}
	
	public static class AttributeTypeItem
	{
		private AttributeType attributeType;

		public AttributeTypeItem(AttributeType value)
		{
			this.attributeType = value;
		}
		
		public String getName()
		{
			return attributeType.getName(); 
		}
		
		AttributeType getAttributeType()
		{
			return attributeType;
		}
	}
}
