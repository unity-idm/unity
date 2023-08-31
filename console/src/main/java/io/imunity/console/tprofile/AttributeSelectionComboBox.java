/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.console.tprofile;

import com.vaadin.flow.component.combobox.ComboBox;
import pl.edu.icm.unity.base.attribute.AttributeType;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.engine.api.AttributeTypeManagement;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Allows to select an attribute name
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
		
		setSizeUndefined();
		setLabel(caption);
		
		List<AttributeType> items = attributeTypes.stream()
			.filter(attrType -> !(filterImmutable && attrType.isInstanceImmutable()))
			.sorted(Comparator.comparing(AttributeType::getName))
			.collect(Collectors.toList());
			
		setItems(items);
		setItemLabelGenerator(AttributeType::getName);

		if (items.size() > 0)
			setValue(items.get(0));
	}
	
	public void setSelectedItemByName(String name)
	{
		if (attributeTypesByName.containsKey(name))
			setValue(attributeTypesByName.get(name));
	}
}
