/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.scim.console;

import com.vaadin.data.Binder;
import com.vaadin.ui.Component;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.ListOfDnDCollapsableElements.Editor;

class AttributeDefinitionWithMappingConfigurationEditor extends Editor<AttributeDefinitionWithMappingBean>
{
	private final MessageSource msg;
	private Binder<AttributeDefinitionWithMappingBean> binder;
	private VerticalLayout main;
	AttributeDefinitionComponent attributeDefinitionComponent;

	AttributeDefinitionWithMappingConfigurationEditor(MessageSource msg, boolean disableMultiAndComplex,
			boolean onlyMappingEdit)
	{
		this.msg = msg;
		init(disableMultiAndComplex, onlyMappingEdit);
	}

	private void init(boolean disableMultiAndComplex, boolean onlyMappingEdit)
	{
		main = new VerticalLayout();
		main.setMargin(false);
		VerticalLayout attrDefHeaderSlot = new VerticalLayout();
		attrDefHeaderSlot.setMargin(false);

		VerticalLayout subAttrSlot = new VerticalLayout();
		subAttrSlot.setMargin(false);

		binder = new Binder<>(AttributeDefinitionWithMappingBean.class);
		attributeDefinitionComponent = new AttributeDefinitionComponent(msg, onlyMappingEdit, disableMultiAndComplex,
				attrDefHeaderSlot, subAttrSlot);
		main.addComponent(attrDefHeaderSlot);

		AttributeMappingComponent attributeMappingComponent = new AttributeMappingComponent(msg);
		attributeDefinitionComponent.addValueChangeListener(e -> attributeMappingComponent.update(e.getValue()));

		main.addComponent(attributeMappingComponent);

		main.addComponent(subAttrSlot);

		binder.forField(attributeDefinitionComponent).asRequired().bind("attributeDefinition");
		binder.addValueChangeListener(
				e -> fireEvent(new ValueChangeEvent<>(this, binder.getBean(), e.isUserOriginated())));
		binder.setValidatorsDisabled(true);
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
}
