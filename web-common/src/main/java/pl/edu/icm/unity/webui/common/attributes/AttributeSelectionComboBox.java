/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.attributes;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.vaadin.ui.ComboBox;

import pl.edu.icm.unity.engine.api.AttributeTypeManagement;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.basic.AttributeType;

/**
 * Allows to select an attribute name
 * @author K. Benedyczak
 */
public class AttributeSelectionComboBox extends ComboBox<AttributeType>
{
	protected Map<String, AttributeType> attributeTypesByName;
	private boolean filterImmutable = true;

	public AttributeSelectionComboBox(String caption, AttributeTypeManagement aTypeMan) throws EngineException
	{
		Collection<AttributeType> attributeTypes = aTypeMan.getAttributeTypes();
		initContents(caption, attributeTypes);		
	}
	
	public AttributeSelectionComboBox(String caption, Collection<AttributeType> attributeTypes, 
			boolean filterImmutable)
	{
		this.filterImmutable = filterImmutable;
		initContents(caption, attributeTypes);
	}

	public AttributeSelectionComboBox(String caption, Collection<AttributeType> attributeTypes)
	{
		this(caption, attributeTypes, true);
	}
	
	private void initContents(String caption, Collection<AttributeType> attributeTypes)
	{
		this.attributeTypesByName = attributeTypes.stream()
				.collect(Collectors.toMap(AttributeType::getName, Function.identity()));
		
		setEmptySelectionAllowed(false);
		setSizeUndefined();
		setCaption(caption);
		
		List<AttributeType> items = attributeTypes.stream()
			.filter(attrType -> !(filterImmutable && attrType.isInstanceImmutable()))
			.sorted((attrType1, attrType2) -> attrType1.getName().compareTo(attrType2.getName()))
			.collect(Collectors.toList());
			
		setItems(items);
		setItemCaptionGenerator(AttributeType::getName);

		if (items.size() > 0)
			setSelectedItem(items.get(0));
	}
	
	public void setSelectedItemByName(String name)
	{
		if (attributeTypesByName.containsKey(name))
			setSelectedItem(attributeTypesByName.get(name));
	}
}
