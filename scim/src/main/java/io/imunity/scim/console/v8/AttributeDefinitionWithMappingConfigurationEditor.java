/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.scim.console.v8;

import java.util.function.Supplier;

import com.vaadin.data.Binder;
import com.vaadin.ui.Component;
import com.vaadin.ui.VerticalLayout;

import io.imunity.scim.console.mapping.AttributeDefinitionWithMappingBean;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.ListOfDnDCollapsableElements.Editor;

class AttributeDefinitionWithMappingConfigurationEditor extends Editor<AttributeDefinitionWithMappingBean>
{
	private final MessageSource msg;
	private Binder<AttributeDefinitionWithMappingBean> binder;
	private VerticalLayout main;
	private AttributeDefinitionComponent attributeDefinitionComponent;
	private final Supplier<AttributeEditContext> context;
	private final AttributeEditorData editorData;
	private AttributeMappingComponent attributeMappingComponent;

	AttributeDefinitionWithMappingConfigurationEditor(MessageSource msg, Supplier<AttributeEditContext> context,
			AttributeEditorData editorData)
	{
		this.msg = msg;
		this.context = context;
		this.editorData = editorData;
		init();
	}

	private void init()
	{
		main = new VerticalLayout();
		main.setMargin(false);
		VerticalLayout attrDefHeaderSlot = new VerticalLayout();
		attrDefHeaderSlot.setMargin(false);

		VerticalLayout subAttrSlot = new VerticalLayout();
		subAttrSlot.setMargin(false);

		binder = new Binder<>(AttributeDefinitionWithMappingBean.class);
		attributeDefinitionComponent = new AttributeDefinitionComponent(msg, context.get(), editorData, attrDefHeaderSlot,
				subAttrSlot);
		main.addComponent(attrDefHeaderSlot);
		attributeMappingComponent = new AttributeMappingComponent(msg, editorData, context);
		attributeDefinitionComponent.addValueChangeListener(e -> attributeMappingComponent.update(e.getValue()));
		main.addComponent(attributeMappingComponent);
		main.addComponent(subAttrSlot);
		binder.forField(attributeDefinitionComponent).asRequired().bind("attributeDefinition");
		binder.addValueChangeListener(
				e -> fireEvent(new ValueChangeEvent<>(this, binder.getBean(), e.isUserOriginated())));
		binder.setValidatorsDisabled(true);
		binder.forField(attributeMappingComponent).bind("attributeMapping");
		attributeMappingComponent.setVisible(!context.get().attributesEditMode.equals(AttributesEditMode.HIDE_MAPPING));

	}

	@Override
	protected String getHeaderText()
	{
		return attributeDefinitionComponent.getHeaderText();

	}

	@Override
	protected void validate() throws FormValidationException
	{
		if (binder.validate().hasErrors())
		{
			throw new FormValidationException();
		}
	}

	@Override
	public AttributeDefinitionWithMappingBean getValue()
	{
		if (binder.validate().hasErrors())
			return null;
		return binder.getBean();
	}

	@Override
	protected Component initContent()
	{
		binder.setValidatorsDisabled(false);
		return main;
	}

	@Override
	protected void doSetValue(AttributeDefinitionWithMappingBean value)
	{
		binder.setBean(value);
	}
	
	void refresh()
	{
		attributeMappingComponent.update(attributeDefinitionComponent.getValue());
	}
}
