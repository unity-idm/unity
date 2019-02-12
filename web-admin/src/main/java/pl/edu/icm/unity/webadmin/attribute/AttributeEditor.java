/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.attribute;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;

import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.HorizontalSplitPanel;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.webadmin.attribute.AttributeMetaEditorPanel.TypeChangeCallback;
import pl.edu.icm.unity.webui.common.CompactFormLayout;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.common.attributes.edit.AttributeEditContext;
import pl.edu.icm.unity.webui.common.attributes.edit.FixedAttributeEditor;
import pl.edu.icm.unity.webui.common.attributes.edit.AttributeEditContext.ConfirmationMode;
import pl.edu.icm.unity.webui.common.attributes.AttributeHandlerRegistry;

/**
 * Allows for editing an attribute or for creating a new one.
 * @author K. Benedyczak
 */
public class AttributeEditor extends CustomComponent
{
	private FixedAttributeEditor valuesPanel;
	private FormLayout attrValuesContainer;
	private AttributeMetaEditorPanel attrTypePanel;
	private String groupPath;
	private boolean typeFixed = false;
	
	/**
	 * For creating a new attribute of arbitrary type.
	 * @param msg
	 * @param attributeTypes
	 * @param groupPath
	 * @param handlerRegistry
	 */
	public AttributeEditor(final UnityMessageSource msg, Collection<AttributeType> attributeTypes, EntityParam owner, String groupPath,
			final AttributeHandlerRegistry handlerRegistry, final boolean required)
	{
		this.groupPath = groupPath;
		attrTypePanel = new AttributeMetaEditorPanel(attributeTypes, groupPath, msg);
		AttributeType initial = attrTypePanel.getAttributeType();
		attrValuesContainer = new CompactFormLayout();
		
		
		AttributeEditContext editContext = AttributeEditContext.builder()
				.withConfirmationMode(ConfirmationMode.ADMIN).withRequired(required)
				.withAttributeType(initial)
				.withAttributeGroup(AttributeEditor.this.groupPath)
				.withAttributeOwner(owner).build();

		valuesPanel = new FixedAttributeEditor(msg, handlerRegistry, editContext, 
				false, null, null);
		valuesPanel.placeOnLayout(attrValuesContainer);
		
		attrTypePanel.setCallback(new TypeChangeCallback()
		{
			@Override
			public void attributeTypeChanged(AttributeType newType)
			{
				
				attrValuesContainer.removeAllComponents();
				AttributeEditContext newEditContext = AttributeEditContext.builder()
						.withConfirmationMode(ConfirmationMode.ADMIN).withRequired(required)
						.withAttributeType(newType)
						.withAttributeGroup(AttributeEditor.this.groupPath)
						.withAttributeOwner(owner).build();

				valuesPanel = new FixedAttributeEditor(msg, handlerRegistry, newEditContext, 
						false, null, null);
				valuesPanel.placeOnLayout(attrValuesContainer);
			}
		});
		initCommon();
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
	public AttributeEditor(UnityMessageSource msg, AttributeType attributeType, Attribute attribute, EntityParam owner, 
			AttributeHandlerRegistry handlerRegistry)
	{
		this.groupPath = attribute.getGroupPath();
		attrTypePanel = new AttributeMetaEditorPanel(attributeType, groupPath, msg);
		attrValuesContainer = new CompactFormLayout();
		
		AttributeEditContext editContext = AttributeEditContext.builder()
				.withConfirmationMode(ConfirmationMode.ADMIN).required()
				.withAttributeType(attributeType)
				.withAttributeGroup(AttributeEditor.this.groupPath)
				.withAttributeOwner(owner).build();
		
		valuesPanel = new FixedAttributeEditor(msg, handlerRegistry, editContext, false, null, null);
		valuesPanel.placeOnLayout(attrValuesContainer);
		valuesPanel.setAttributeValues(attribute.getValues());
		initCommon();
	}

	/**
	 * For creating a new attribute but with a fixed type.
	 * @param msg
	 * @param attributeType
	 * @param attribute
	 * @param handlerRegistry
	 */
	public AttributeEditor(UnityMessageSource msg, AttributeType attributeType, EntityParam owner, String groupPath, 
			AttributeHandlerRegistry handlerRegistry)
	{
		this.groupPath = groupPath;
		attrTypePanel = new AttributeMetaEditorPanel(attributeType, groupPath, msg);
		attrValuesContainer = new CompactFormLayout();
		
		AttributeEditContext editContext = AttributeEditContext.builder()
				.withConfirmationMode(ConfirmationMode.ADMIN).required()
				.withAttributeType(attributeType)
				.withAttributeGroup(AttributeEditor.this.groupPath)
				.withAttributeOwner(owner).build();

		valuesPanel = new FixedAttributeEditor(msg, handlerRegistry, editContext, false, null, null);
		valuesPanel.placeOnLayout(attrValuesContainer);
		typeFixed = true;
		initCommon();
	}
	
	private void initCommon()
	{
		HorizontalSplitPanel split = new HorizontalSplitPanel(attrTypePanel, attrValuesContainer);
		split.setSplitPosition(45);
		attrValuesContainer.setMargin(new MarginInfo(true, true, true, true));
		attrValuesContainer.setSizeUndefined();
		attrTypePanel.setMargin(new MarginInfo(false, true, false, false));
		setCompositionRoot(split);
		split.addStyleName(Styles.visibleScroll.toString());
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

