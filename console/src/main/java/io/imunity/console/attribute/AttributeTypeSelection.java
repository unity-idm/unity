/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.console.attribute;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.shared.HasTooltip;
import io.imunity.console_utils.tprofile.AttributeSelectionComboBox;
import io.imunity.vaadin.elements.TooltipFactory;
import org.apache.commons.lang3.StringUtils;
import pl.edu.icm.unity.base.attribute.AttributeType;
import pl.edu.icm.unity.base.message.MessageSource;

import java.util.Collection;
import java.util.Collections;
import java.util.function.Consumer;

import static io.imunity.vaadin.elements.CSSVars.TEXT_FIELD_MEDIUM;

class AttributeTypeSelection extends FormLayout
{
	private final MessageSource msg;
	private Consumer<AttributeType> callback;
	private FormLayout.FormItem formItem;
	private Component formItemTooltip;
	private AttributeSelectionComboBox attributeTypesCombo;

	AttributeTypeSelection(AttributeType attributeType, MessageSource msg)
	{
		this(Collections.singletonList(attributeType), msg);
	}

	AttributeTypeSelection(Collection<AttributeType> attributeTypes, MessageSource msg)
	{
		this.msg = msg;
		createAttributeSelectionWidget(attributeTypes);
	}

	private void createAttributeWidget(AttributeType type)
	{
		Span name = new Span(type.getName());
		formItem = addFormItem(name, msg.getMessage("AttributeType.name"));
		String message = type.getDescription().getValue(msg);
		formItemTooltip = TooltipFactory.get(message);
		formItemTooltip.setVisible(!StringUtils.isEmpty(message));
		formItem.add(formItemTooltip);
	}

	private void createAttributeSelectionWidget(Collection<AttributeType> attributeTypes)
	{
		attributeTypesCombo = new AttributeSelectionComboBox(null, attributeTypes);
		attributeTypesCombo.setWidth(TEXT_FIELD_MEDIUM.value());
		if (attributeTypes.size() == 1)
		{
			createAttributeWidget(attributeTypes.iterator().next());
		} else
		{
			formItem = addFormItem(attributeTypesCombo, msg.getMessage("AttributeType.name"));
			formItemTooltip = TooltipFactory.get("");
			formItemTooltip.setVisible(false);
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
		String message = type.getDescription().getValue(msg);
		((HasTooltip)formItemTooltip).setTooltipText(message);
		formItemTooltip.setVisible(!StringUtils.isEmpty(message));
		if (callback != null)
			callback.accept(type);
	}
}
