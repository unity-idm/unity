/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.webconsole.attribute;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;

import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Panel;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.base.attribute.Attribute;
import pl.edu.icm.unity.base.attribute.AttributeType;
import pl.edu.icm.unity.base.entity.EntityParam;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.webui.common.ConfirmationEditMode;
import pl.edu.icm.unity.webui.common.FormLayoutWithFixedCaptionWidth;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.attributes.AttributeHandlerRegistryV8;
import pl.edu.icm.unity.webui.common.attributes.edit.AttributeEditContext;
import pl.edu.icm.unity.webui.common.attributes.edit.FixedAttributeEditor;

/**
 * Allows for editing an attribute or for creating a new one.
 * @author K. Benedyczak
 */
public class AttributeEditor extends CustomComponent
{
	private FixedAttributeEditor valuesPanel;
	private FormLayout attrValuesContainer;
	private AttributeTypeSelection attrTypePanel;
	private String groupPath;
	private boolean typeFixed = false;
	
	/**
	 * For creating a new attribute of arbitrary type.
	 * @param msg
	 * @param attributeTypes
	 * @param groupPath
	 * @param handlerRegistry
	 */
	public AttributeEditor(final MessageSource msg, Collection<AttributeType> attributeTypes, EntityParam owner, String groupPath,
	                       final AttributeHandlerRegistryV8 handlerRegistry, final boolean required)
	{
		this.groupPath = groupPath;
		attrTypePanel = new AttributeTypeSelection(attributeTypes, groupPath, msg);
		AttributeType initial = attrTypePanel.getAttributeType();
		attrValuesContainer = FormLayoutWithFixedCaptionWidth.withMediumCaptions();
	
		AttributeEditContext editContext = AttributeEditContext.builder()
				.withConfirmationMode(ConfirmationEditMode.ADMIN).withRequired(required)
				.withAttributeType(initial)
				.withAttributeGroup(AttributeEditor.this.groupPath)
				.withAttributeOwner(owner)
				.withCustomWidth(100)
				.withCustomWidthUnit(Unit.PERCENTAGE)
				.build();

		valuesPanel = new FixedAttributeEditor(msg, handlerRegistry, editContext, 
				false, null, null);
		valuesPanel.placeOnLayout(attrValuesContainer);
		
		attrTypePanel.setCallback(newType ->
		{
			attrValuesContainer.removeAllComponents();
			AttributeEditContext newEditContext = AttributeEditContext.builder()
					.withConfirmationMode(ConfirmationEditMode.ADMIN).withRequired(required).withAttributeType(newType)
					.withAttributeGroup(AttributeEditor.this.groupPath).withAttributeOwner(owner)
					.withCustomWidth(100)
					.withCustomWidthUnit(Unit.PERCENTAGE)
					.build();

			valuesPanel = new FixedAttributeEditor(msg, handlerRegistry, newEditContext, false, null, null);
			valuesPanel.placeOnLayout(attrValuesContainer);
		});
		initCommon(msg);
	}
	
	/**
	 * Useful in the full edit mode (when choice of attributes is allowed). Sets the initial attribute.
	 * @param attribute
	 */
	public void setInitialAttribute(Attribute attribute)
	{
		if (!typeFixed)
			attrTypePanel.setAttributeType(attribute.getName());
		valuesPanel.setAttributeValues(attribute.getValues());
	}

	/**
	 * For editing an existing attribute - the type is fixed.
	 * @param msg
	 * @param attributeType
	 * @param attribute
	 * @param handlerRegistry
	 */
	public AttributeEditor(MessageSource msg, AttributeType attributeType, Attribute attribute, EntityParam owner, 
			AttributeHandlerRegistryV8 handlerRegistry)
	{
		this.groupPath = attribute.getGroupPath();
		attrTypePanel = new AttributeTypeSelection(attributeType, groupPath, msg);
		attrValuesContainer = FormLayoutWithFixedCaptionWidth.withMediumCaptions();
		
		AttributeEditContext editContext = AttributeEditContext.builder()
				.withConfirmationMode(ConfirmationEditMode.ADMIN).required()
				.withAttributeType(attributeType)
				.withAttributeGroup(AttributeEditor.this.groupPath)
				.withAttributeOwner(owner)
				.withCustomWidth(100)
				.withCustomWidthUnit(Unit.PERCENTAGE)
				.build();
		
		valuesPanel = new FixedAttributeEditor(msg, handlerRegistry, editContext, false, null, null);
		valuesPanel.placeOnLayout(attrValuesContainer);
		valuesPanel.setAttributeValues(attribute.getValues());
		initCommon(msg);
	}

	/**
	 * For creating a new attribute but with a fixed type.
	 * @param msg
	 * @param attributeType
	 * @param attribute
	 * @param handlerRegistry
	 */
	public AttributeEditor(MessageSource msg, AttributeType attributeType, EntityParam owner, String groupPath, 
			AttributeHandlerRegistryV8 handlerRegistry)
	{
		this.groupPath = groupPath;
		attrTypePanel = new AttributeTypeSelection(attributeType, groupPath, msg);
		attrValuesContainer = FormLayoutWithFixedCaptionWidth.withMediumCaptions();
		
		AttributeEditContext editContext = AttributeEditContext.builder()
				.withConfirmationMode(ConfirmationEditMode.ADMIN).required()
				.withAttributeType(attributeType)
				.withAttributeGroup(AttributeEditor.this.groupPath)
				.withAttributeOwner(owner)
				.withCustomWidth(100)
				.withCustomWidthUnit(Unit.PERCENTAGE)
				.build();

		valuesPanel = new FixedAttributeEditor(msg, handlerRegistry, editContext, false, null, null);
		valuesPanel.placeOnLayout(attrValuesContainer);
		typeFixed = true;
		initCommon(msg);
	}
	
	private void initCommon(MessageSource msg)
	{
		VerticalLayout main = new VerticalLayout();
		main.setMargin(false);
		main.setSpacing(false);
		main.addComponent(attrTypePanel);		
		Panel panel = new Panel();
		panel.setContent(attrValuesContainer);
		VerticalLayout wrap = new VerticalLayout();
		wrap.setMargin(false);
		FormLayoutWithFixedCaptionWidth wrapper = FormLayoutWithFixedCaptionWidth.withVeryShortCaptions();		
		wrap.addComponent(panel);
		wrapper.addComponent(wrap);
		main.addComponent(wrapper);
		attrValuesContainer.setWidthFull();
		setCompositionRoot(main);
	}
	
	public Attribute getAttribute() throws FormValidationException
	{
		Optional<Attribute> ret = valuesPanel.getAttribute();
		if (!ret.isPresent())
		{
			AttributeType at = attrTypePanel.getAttributeType();
			return new Attribute(at.getName(), at.getValueSyntax(), groupPath, 
					new ArrayList<>());
		}
		return ret.get();
	}

}

