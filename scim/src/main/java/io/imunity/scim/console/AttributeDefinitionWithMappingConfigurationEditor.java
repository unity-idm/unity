/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.scim.console;

import java.util.function.Supplier;

import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.binder.Binder;

import io.imunity.scim.console.mapping.AttributeDefinitionWithMappingBean;
import io.imunity.vaadin.endpoint.common.api.HtmlTooltipFactory;
import io.imunity.vaadin.auth.services.idp.CollapsableGrid.Editor;
import pl.edu.icm.unity.base.message.MessageSource;
import io.imunity.vaadin.endpoint.common.exceptions.FormValidationException;

class AttributeDefinitionWithMappingConfigurationEditor extends Editor<AttributeDefinitionWithMappingBean>
{
	private final MessageSource msg;
	private final HtmlTooltipFactory tooltipFactory;
	private final Supplier<AttributeEditContext> context;
	private final AttributeEditorData editorData;
	private AttributeMappingComponent attributeMappingComponent;
	private AttributeDefinitionComponent attributeDefinitionComponent;
	private Binder<AttributeDefinitionWithMappingBean> binder;
	private VerticalLayout main;
	
	AttributeDefinitionWithMappingConfigurationEditor(MessageSource msg, HtmlTooltipFactory tooltipFactory, Supplier<AttributeEditContext> context,
			AttributeEditorData editorData)
	{
		this.msg = msg;
		this.tooltipFactory = tooltipFactory;
		this.context = context;
		this.editorData = editorData;
		init();
	}

	private void init()
	{
		main = new VerticalLayout();
		main.setMargin(false);
		main.setPadding(false);
		main.setSpacing(false);
		
		VerticalLayout attrDefHeaderSlot = new VerticalLayout();
		attrDefHeaderSlot.setMargin(false);
		attrDefHeaderSlot.setPadding(false);
	
		VerticalLayout subAttrSlot = new VerticalLayout();
		subAttrSlot.setMargin(false);
		subAttrSlot.setPadding(false);

		binder = new Binder<>(AttributeDefinitionWithMappingBean.class);
		attributeDefinitionComponent = new AttributeDefinitionComponent(msg, tooltipFactory, context.get(), editorData, attrDefHeaderSlot,
				subAttrSlot);
		main.add(attrDefHeaderSlot);
		attributeMappingComponent = new AttributeMappingComponent(msg, tooltipFactory, editorData, context);
		attributeDefinitionComponent.addValueChangeListener(e -> attributeMappingComponent.update(e.getValue()));
		main.add(attributeMappingComponent);
		main.add(subAttrSlot);
		binder.forField(attributeDefinitionComponent).asRequired().bind("attributeDefinition");
		binder.addValueChangeListener(e -> fireEvent(new ComponentValueChangeEvent<>(this, this, getValue(), e.isFromClient())));
		binder.forField(attributeMappingComponent).bind("attributeMapping");
		attributeMappingComponent.setVisible(!context.get().attributesEditMode.equals(AttributesEditMode.HIDE_MAPPING));
		addAttachListener(e -> attributeDefinitionComponent.setValidatorsDisabled(false));
		add(main);
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

	void refresh()
	{
		attributeMappingComponent.update(attributeDefinitionComponent.getValue());
	}

	@Override
	protected AttributeDefinitionWithMappingBean generateModelValue()
	{
		return getValue();
	}

	@Override
	protected void setPresentationValue(AttributeDefinitionWithMappingBean newPresentationValue)
	{
		binder.setBean(newPresentationValue);	
	}
}
