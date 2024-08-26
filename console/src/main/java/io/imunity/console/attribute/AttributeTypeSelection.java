/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.console.attribute;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;

import io.imunity.console.tprofile.AttributeSelectionComboBox;
import io.imunity.vaadin.endpoint.common.TooltipFactory;

import org.apache.commons.lang3.StringUtils;
import pl.edu.icm.unity.base.attribute.AttributeType;
import pl.edu.icm.unity.base.message.MessageSource;

import java.util.Collection;
import java.util.Collections;
import java.util.function.Consumer;

import static io.imunity.vaadin.elements.CSSVars.TEXT_FIELD_MEDIUM;
import static org.apache.commons.lang3.StringUtils.isNoneEmpty;

class AttributeTypeSelection extends VerticalLayout
{
	private final MessageSource msg;
	private Consumer<AttributeType> callback;
	private HorizontalLayout formItem;
	private Component formItemTooltip;
	private AttributeSelectionComboBox attributeTypesCombo;

	AttributeTypeSelection(AttributeType attributeType, MessageSource msg)
	{
		this(Collections.singletonList(attributeType), msg);
	}

	AttributeTypeSelection(Collection<AttributeType> attributeTypes, MessageSource msg)
	{
		this.msg = msg;
		setSpacing(false);
		setPadding(false);
		createAttributeSelectionWidget(attributeTypes);
	}

	private void createAttributeWidget(AttributeType type)
	{
		TextField name = new TextField(msg.getMessage("AttributeType.name"));
		name.setWidth(TEXT_FIELD_MEDIUM.value());
		name.setValue(type.getName());
		name.setReadOnly(true);
		formItem = new HorizontalLayout(name);
		formItem.setSpacing(false);
		formItem.setAlignItems(Alignment.END);
		add(formItem);
		String message = type.getDescription().getValue(msg);
		setTooltipComponent(message);
		formItemTooltip.setVisible(isNoneEmpty(message));
		formItem.add(formItemTooltip);
	}

	private void createAttributeSelectionWidget(Collection<AttributeType> attributeTypes)
	{
		attributeTypesCombo = new AttributeSelectionComboBox(null, attributeTypes, msg);
		attributeTypesCombo.setWidth(TEXT_FIELD_MEDIUM.value());
		if (attributeTypes.size() == 1)
		{
			createAttributeWidget(attributeTypes.iterator().next());
		} else
		{
			attributeTypesCombo.setLabel(msg.getMessage("AttributeType.name"));
			formItem = new HorizontalLayout(attributeTypesCombo);
			formItem.setAlignItems(Alignment.END);
			formItem.setSpacing(false);
			add(formItem);
			AttributeType value = attributeTypesCombo.getValue();
			if (value != null)
			{
				String message = value.getDescription().getValue(msg);
				setTooltipComponent(message);
				formItemTooltip.setVisible(isNoneEmpty(message));
			}
			formItem.add(formItemTooltip);
			attributeTypesCombo.addValueChangeListener(event -> changeAttributeType(event.getValue()));
		}
	}

	public void setAttributeType(String name2)
	{
		attributeTypesCombo.setSelectedItemByName(name2);
	}

	public AttributeType getAttributeType()
	{
		return attributeTypesCombo.getValue();
	}

	void setCallback(Consumer<AttributeType> callback)
	{
		this.callback = callback;
	}

	private void changeAttributeType(AttributeType type)
	{
		if(type == null)
			return;
		String message = type.getDescription().getValue(msg);
		formItem.remove(formItemTooltip);
		setTooltipComponent(message);
		formItem.add(formItemTooltip);
		formItemTooltip.setVisible(!StringUtils.isEmpty(message));
		if (callback != null)
			callback.accept(type);
	}

	private void setTooltipComponent(String message)
	{
		formItemTooltip = TooltipFactory.getWithHtmlContent(message);
		formItemTooltip.getStyle().set("margin-bottom", "0.9em");
	}
}
