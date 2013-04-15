/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.attribute;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.AttributeValueSyntax;
import pl.edu.icm.unity.webadmin.attribute.AttributeMetaEditorPanel.TypeChangeCallback;
import pl.edu.icm.unity.webui.common.attributes.AttributeHandlerRegistry;
import pl.edu.icm.unity.webui.common.attributes.WebAttributeHandler;

import com.vaadin.ui.Alignment;
import com.vaadin.ui.HorizontalLayout;

/**
 * Allows for editing an attribute or for creating a new one.
 * @author K. Benedyczak
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
public class AttributeEditor extends HorizontalLayout
{
	private ValuesEditorPanel<?> valuesPanel;
	private AttributeMetaEditorPanel attrTypePanel;
	private String groupPath;
	
	public AttributeEditor(final UnityMessageSource msg, Collection<AttributeType> attributeTypes, String groupPath,
			final AttributeHandlerRegistry handlerRegistry)
	{
		this.groupPath = groupPath;
		attrTypePanel = new AttributeMetaEditorPanel(attributeTypes, groupPath, msg);
		AttributeType initial = attrTypePanel.getAttributeType();
		WebAttributeHandler<?> handler = handlerRegistry.getHandler(initial.getValueType().getValueSyntaxId());
		valuesPanel = new ValuesEditorPanel(msg, Collections.emptyList(), initial.getValueType(), handler);

		attrTypePanel.setCallback(new TypeChangeCallback()
		{
			@Override
			public void attributeTypeChanged(AttributeType newType)
			{
				removeComponent(valuesPanel);
				WebAttributeHandler<?> handler = handlerRegistry.getHandler(
						newType.getValueType().getValueSyntaxId());
				valuesPanel = new ValuesEditorPanel(msg, Collections.emptyList(), 
						newType.getValueType(), handler);
				addComponent(valuesPanel);
				setComponentAlignment(valuesPanel, Alignment.TOP_LEFT);
			}
		});
		initCommon();
		
	}

	public AttributeEditor(UnityMessageSource msg, AttributeType attributeType, Attribute<?> attribute, 
			AttributeHandlerRegistry handlerRegistry)
	{
		this.groupPath = attribute.getGroupPath();
		attrTypePanel = new AttributeMetaEditorPanel(attributeType, groupPath, msg, attribute.getVisibility());
		AttributeValueSyntax<?> syntax = attributeType.getValueType();
		WebAttributeHandler<?> handler = handlerRegistry.getHandler(syntax.getValueSyntaxId());
		valuesPanel = new ValuesEditorPanel(msg, attribute.getValues(), syntax, handler);
		initCommon();
	}
	
	private void initCommon()
	{
		setSpacing(true);
		addComponent(attrTypePanel);
		setComponentAlignment(attrTypePanel, Alignment.TOP_RIGHT);
		addComponent(valuesPanel);
		setComponentAlignment(valuesPanel, Alignment.TOP_LEFT);
		setSizeFull();
	}
	
	public Attribute<?> getAttribute()
	{
		AttributeType attrType = attrTypePanel.getAttributeType();
		List<?> values = valuesPanel.getValues();
		return new Attribute(attrType.getName(), attrType.getValueType(), groupPath, 
				attrTypePanel.getVisibility(), values);
	}

}

