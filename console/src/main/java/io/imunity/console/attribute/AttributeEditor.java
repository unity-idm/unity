/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.console.attribute;

import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import io.imunity.vaadin.elements.Panel;
import io.imunity.vaadin.endpoint.common.plugins.attributes.*;
import pl.edu.icm.unity.base.attribute.Attribute;
import pl.edu.icm.unity.base.attribute.AttributeType;
import pl.edu.icm.unity.base.entity.EntityParam;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.webui.common.FormValidationException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;

public class AttributeEditor extends VerticalLayout
{
	private final FormLayout attrValuesContainer;
	private final AttributeTypeSelection attrTypePanel;
	private final String groupPath;
	private boolean typeFixed = false;
	private FixedAttributeEditor valuesPanel;

	public AttributeEditor(MessageSource msg, Collection<AttributeType> attributeTypes, EntityParam owner, String groupPath,
	                       AttributeHandlerRegistry handlerRegistry, boolean required)
	{
		this.groupPath = groupPath;
		attrTypePanel = new AttributeTypeSelection(attributeTypes, msg);
		attrTypePanel.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
		AttributeType initial = attrTypePanel.getAttributeType();
		attrValuesContainer = new FormLayout();
	
		AttributeEditContext editContext = AttributeEditContext.builder()
				.withConfirmationMode(ConfirmationEditMode.ADMIN).withRequired(required)
				.withAttributeType(initial)
				.withAttributeGroup(AttributeEditor.this.groupPath)
				.withAttributeOwner(owner)
				.withCustomWidth(100)
				.withCustomWidthUnit(Unit.PERCENTAGE)
				.build();

		valuesPanel = new FixedAttributeEditor(msg, handlerRegistry, editContext,
				 new LabelContext(initial.getDisplayedName().getValue(msg) + ":"), null);
		valuesPanel.placeOnLayout(attrValuesContainer);
		
		attrTypePanel.setCallback(newType ->
		{
			attrValuesContainer.removeAll();
			AttributeEditContext newEditContext = AttributeEditContext.builder()
					.withConfirmationMode(ConfirmationEditMode.ADMIN).withRequired(required).withAttributeType(newType)
					.withAttributeGroup(AttributeEditor.this.groupPath).withAttributeOwner(owner)
					.withCustomWidth(100)
					.withCustomWidthUnit(Unit.PERCENTAGE)
					.build();

			valuesPanel = new FixedAttributeEditor(msg, handlerRegistry, newEditContext,
					new LabelContext(newType.getDisplayedName().getValue(msg) + ":"),
					null);
			valuesPanel.placeOnLayout(attrValuesContainer);
		});
		setSizeFull();
		initCommon();
	}
	

	public void setInitialAttribute(Attribute attribute)
	{
		if (!typeFixed)
			attrTypePanel.setAttributeType(attribute.getName());
		valuesPanel.setAttributeValues(attribute.getValues());
	}

	public AttributeEditor(MessageSource msg, AttributeType attributeType, Attribute attribute, EntityParam owner, 
			AttributeHandlerRegistry handlerRegistry)
	{
		this.groupPath = attribute.getGroupPath();
		attrTypePanel = new AttributeTypeSelection(attributeType, msg);
		attrTypePanel.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
		attrValuesContainer = new FormLayout();
		
		AttributeEditContext editContext = AttributeEditContext.builder()
				.withConfirmationMode(ConfirmationEditMode.ADMIN).required()
				.withAttributeType(attributeType)
				.withAttributeGroup(AttributeEditor.this.groupPath)
				.withAttributeOwner(owner)
				.withCustomWidth(100)
				.withCustomWidthUnit(Unit.PERCENTAGE)
				.build();
		
		valuesPanel = new FixedAttributeEditor(msg, handlerRegistry, editContext,
				new LabelContext(attributeType.getDisplayedName().getValue(msg) + ":"),
				null);
		valuesPanel.placeOnLayout(attrValuesContainer);
		valuesPanel.setAttributeValues(attribute.getValues());
		initCommon();
	}

	public AttributeEditor(MessageSource msg, AttributeType attributeType, EntityParam owner, String groupPath, 
			AttributeHandlerRegistry handlerRegistry)
	{
		this.groupPath = groupPath;
		attrTypePanel = new AttributeTypeSelection(attributeType, msg);
		attrValuesContainer = new FormLayout();
		attrTypePanel.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));

		AttributeEditContext editContext = AttributeEditContext.builder()
				.withConfirmationMode(ConfirmationEditMode.ADMIN).required()
				.withAttributeType(attributeType)
				.withAttributeGroup(AttributeEditor.this.groupPath)
				.withAttributeOwner(owner)
				.withCustomWidth(100)
				.withCustomWidthUnit(Unit.PERCENTAGE)
				.build();

		valuesPanel = new FixedAttributeEditor(msg, handlerRegistry, editContext,  null, null);
		valuesPanel.placeOnLayout(attrValuesContainer);
		typeFixed = true;
		initCommon();
	}
	
	private void initCommon()
	{
		removeAll();
		Panel panel = new Panel(Optional.empty());
		panel.add(attrValuesContainer);
		panel.setMargin(false);
		attrTypePanel.addFormItem(panel, "");
		add(attrTypePanel);
	}
	
	public Attribute getAttribute() throws FormValidationException
	{
		Optional<Attribute> ret = valuesPanel.getAttribute();
		if (ret.isEmpty())
		{
			AttributeType at = attrTypePanel.getAttributeType();
			return new Attribute(at.getName(), at.getValueSyntax(), groupPath, 
					new ArrayList<>());
		}
		return ret.get();
	}

}

