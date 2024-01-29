/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.console.tprofile;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.vaadin.flow.component.AbstractField.ComponentValueChangeEvent;
import com.vaadin.flow.component.HasValueAndElement;
import com.vaadin.flow.component.combobox.ComboBox;

import io.imunity.vaadin.elements.NotEmptyComboBox;
import pl.edu.icm.unity.base.attribute.AttributeType;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.engine.api.AttributeTypeManagement;

/**
 * Allows to select an attribute name
 */
public class AttributeSelectionComboBox extends NotEmptyComboBox<AttributeType> implements
		HasValueAndElement<ComponentValueChangeEvent<ComboBox<AttributeType>, AttributeType>, AttributeType>
{
	protected Map<String, AttributeType> attributeTypesByName;
	private boolean filterImmutable = true;
	private String label;

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
		this.label = caption;
		
		setSizeUndefined();
		setLabel(caption);
		
		List<AttributeType> items = attributeTypes.stream()
			.filter(attrType -> !(filterImmutable && attrType.isInstanceImmutable()))
			.sorted(Comparator.comparing(AttributeType::getName))
			.collect(Collectors.toList());
			
		setItems(items);
		setItemLabelGenerator(AttributeType::getName);

		if (!items.isEmpty())
			setValue(items.get(0));
	}
	
	public void setSelectedItemByName(String name)
	{
		if (attributeTypesByName.containsKey(name))
			setValue(attributeTypesByName.get(name));
	}
	
	@Override
	public String getLabel()
	{
		return label;
	}
}
