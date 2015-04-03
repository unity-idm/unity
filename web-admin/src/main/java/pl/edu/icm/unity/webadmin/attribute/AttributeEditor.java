/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.attribute;

import java.util.ArrayList;
import java.util.Collection;

import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.AttributeVisibility;
import pl.edu.icm.unity.webadmin.attribute.AttributeMetaEditorPanel.TypeChangeCallback;
import pl.edu.icm.unity.webui.common.CompactFormLayout;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.attributes.AttributeHandlerRegistry;
import pl.edu.icm.unity.webui.common.attributes.FixedAttributeEditor;

import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.HorizontalLayout;

/**
 * Allows for editing an attribute or for creating a new one.
 * @author K. Benedyczak
 */
public class AttributeEditor extends HorizontalLayout
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
	public AttributeEditor(final UnityMessageSource msg, Collection<AttributeType> attributeTypes, String groupPath,
			final AttributeHandlerRegistry handlerRegistry, final boolean required)
	{
		this.groupPath = groupPath;
		attrTypePanel = new AttributeMetaEditorPanel(attributeTypes, groupPath, msg);
		AttributeType initial = attrTypePanel.getAttributeType();
		attrValuesContainer = new CompactFormLayout();
		valuesPanel = new FixedAttributeEditor(msg, handlerRegistry, initial, 
				false, AttributeEditor.this.groupPath, AttributeVisibility.full, null, null, 
				required, true, attrValuesContainer);

		attrTypePanel.setCallback(new TypeChangeCallback()
		{
			@Override
			public void attributeTypeChanged(AttributeType newType)
			{
				attrValuesContainer.removeAllComponents();
				valuesPanel = new FixedAttributeEditor(msg, handlerRegistry, newType, 
						false, AttributeEditor.this.groupPath, AttributeVisibility.full, 
						null, null, required, true, attrValuesContainer);
			}
		});
		initCommon();
	}
	
	/**
	 * Useful in the full edit mode (when choice of attributes is allowed). Sets the initial attribute.
	 * @param attribute
	 */
	public void setInitialAttribute(Attribute<?> attribute)
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
	public AttributeEditor(UnityMessageSource msg, AttributeType attributeType, Attribute<?> attribute, 
			AttributeHandlerRegistry handlerRegistry)
	{
		this.groupPath = attribute.getGroupPath();
		attrTypePanel = new AttributeMetaEditorPanel(attributeType, groupPath, msg, attribute.getVisibility());
		attrValuesContainer = new CompactFormLayout();
		valuesPanel = new FixedAttributeEditor(msg, handlerRegistry, attributeType, 
				false, AttributeEditor.this.groupPath, AttributeVisibility.full, null, null, true, true, 
				attrValuesContainer);
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
	public AttributeEditor(UnityMessageSource msg, AttributeType attributeType, String groupPath, 
			AttributeHandlerRegistry handlerRegistry)
	{
		this.groupPath = groupPath;
		attrTypePanel = new AttributeMetaEditorPanel(attributeType, groupPath, msg, attributeType.getVisibility());
		attrValuesContainer = new CompactFormLayout();
		valuesPanel = new FixedAttributeEditor(msg, handlerRegistry, attributeType, 
				false, AttributeEditor.this.groupPath, AttributeVisibility.full, null, null, true, true, 
				attrValuesContainer);
		typeFixed = true;
		initCommon();
	}
	
	private void initCommon()
	{
		setSpacing(true);
		attrValuesContainer.setMargin(new MarginInfo(true, true, true, true));
		attrValuesContainer.setSizeUndefined();
		attrTypePanel.setSizeUndefined();
		addComponent(attrTypePanel);
		setComponentAlignment(attrTypePanel, Alignment.TOP_RIGHT);
		addComponent(attrValuesContainer);
		setComponentAlignment(attrValuesContainer, Alignment.TOP_LEFT);
		setExpandRatio(attrTypePanel, 1.0f);
		setExpandRatio(attrValuesContainer, 1.5f);
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public Attribute<?> getAttribute() throws FormValidationException
	{
		Attribute<?> ret = valuesPanel.getAttribute();
		if (ret == null)
		{
			AttributeType at = attrTypePanel.getAttributeType();
			return new Attribute(at.getName(), at.getValueType(), groupPath, attrTypePanel.getVisibility(), 
					new ArrayList<>());
		}
		ret.setVisibility(attrTypePanel.getVisibility());
		return ret;
	}

}

