/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.console.tprofile;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import io.imunity.vaadin.elements.NotEmptyComboBox;
import pl.edu.icm.unity.base.attribute.AttributeType;
import pl.edu.icm.unity.base.message.MessageSource;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static io.imunity.vaadin.elements.CssClassNames.BOLD;

/**
 * Allows to select an attribute name
 */
public class AttributeSelectionComboBox extends NotEmptyComboBox<AttributeType>
{
	private final MessageSource msg;
	protected Map<String, AttributeType> attributeTypesByName;
	private boolean filterImmutable = true;
	private String label;

	public AttributeSelectionComboBox(String caption, Collection<AttributeType> attributeTypes, boolean filterImmutable,
			MessageSource msg)
	{
		this.msg = msg;
		this.filterImmutable = filterImmutable;
		initContents(caption, attributeTypes);
	}

	public AttributeSelectionComboBox(String caption, Collection<AttributeType> attributeTypes, MessageSource msg)
	{
		this(caption, attributeTypes, true, msg);
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
		setItemLabelGenerator(attributeType -> getLabel(attributeType));
		setRenderer(new ComponentRenderer<>(attributeType ->
		{
			Div displayedName = new Div();
			displayedName.setText(getLabel(attributeType));
			displayedName.addClassName(BOLD.getName());
			Div id = new Div();
			id.setText(attributeType.getName());
			displayedName.add(id);
			return new Div(displayedName, id);
		}));

		if (!items.isEmpty())
			setValue(items.get(0));
	}

	private String getLabel(AttributeType attributeType)
	{
		return attributeType.getDisplayedName() != null ? attributeType.getDisplayedName()
				.getValue(msg)
				.isEmpty() ? attributeType.getName()
						: attributeType.getDisplayedName()
								.getValue(msg)
				: attributeType.getName();
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
