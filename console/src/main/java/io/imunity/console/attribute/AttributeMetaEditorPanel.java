/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.console.attribute;

import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Span;
import io.imunity.console_utils.tprofile.AttributeSelectionComboBox;
import pl.edu.icm.unity.base.attribute.AttributeType;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.webui.common.AttributeTypeUtils;

import java.util.Collection;
import java.util.Collections;

class AttributeMetaEditorPanel extends FormLayout
{
	private final MessageSource msg;
	
	private Span valueType;
	private Html typeDescription;
	
	private String attributeName;
	private Span cardinality;
	private Span unique;
	private AttributeSelectionComboBox attributeTypes;
	private TypeChangeCallback callback;

	AttributeMetaEditorPanel(AttributeType attributeType, String groupPath, MessageSource msg)
	{
		this(Collections.singletonList(attributeType), groupPath, msg);
	}
	
	AttributeMetaEditorPanel(Collection<AttributeType> attributeTypes, String groupPath, 
			MessageSource msg)
	{
		this.msg = msg;
		createAttributeSelectionWidget(attributeTypes);
		AttributeType selected = getAttributeType();
		initCommon(selected, groupPath);
	}
	
	private void initCommon(AttributeType attributeType, String groupPath)
	{
		valueType = new Span(attributeType.getValueSyntax());
		addFormItem(valueType, msg.getMessage("AttributeType.type"));

		typeDescription = new Html("<div>" + attributeType.getDescription().getValue(msg) + "</div>");
		addFormItem(typeDescription, msg.getMessage("AttributeType.description"));

		Span group = new Span(groupPath);
		group.setWidthFull();
		addFormItem(group, msg.getMessage("Attribute.group"));
		
		cardinality = new Span();
		addFormItem(cardinality, msg.getMessage("AttributeType.cardinality"));
		cardinality.setText(AttributeTypeUtils.getBoundsDesc(msg, attributeType.getMinElements(),
				attributeType.getMaxElements()));
		
		unique = new Span();
		addFormItem(unique, msg.getMessage("AttributeType.uniqueValues"));
		unique.setText(AttributeTypeUtils.getBooleanDesc(msg, attributeType.isUniqueValues()));

		setWidthFull();
	}

	private void createAttributeWidget(String attributeName)
	{
		Span name = new Span(attributeName);
		addFormItem(name, msg.getMessage("AttributeType.name"));
	}
	
	private void createAttributeSelectionWidget(Collection<AttributeType> attributeTypes)
	{
		this.attributeTypes = new AttributeSelectionComboBox(msg.getMessage("AttributeType.name"), 
				attributeTypes); 
		
		if (attributeTypes.size() == 1)
		{
			createAttributeWidget(attributeTypes.iterator().next().getName());
		} else
		{
			add(this.attributeTypes);
			this.attributeTypes.setWidthFull();
			this.attributeTypes.addValueChangeListener(event -> changeAttribute());
		}
	}
	
	TypeChangeCallback getCallback()
	{
		return callback;
	}

	void setCallback(TypeChangeCallback callback)
	{
		this.callback = callback;
	}

	private void changeAttribute()
	{
		AttributeType type = attributeTypes.getValue();
		setAttributeType(type);
		if (callback != null)
			callback.attributeTypeChanged(type);
	}

	void setAttributeType(String name)
	{
		attributeTypes.setSelectedItemByName(name);
	}
	
	private void setAttributeType(AttributeType type)
	{
		valueType.setText(type.getValueSyntax());
		
		typeDescription.setHtmlContent("<div>" + type.getDescription().getValue(msg) + "</div>");
		
		cardinality.setText(AttributeTypeUtils.getBoundsDesc(msg, type.getMinElements(),
				type.getMaxElements()));
		unique.setText(AttributeTypeUtils.getBooleanDesc(msg, type.isUniqueValues()));
	}
	
	String getAttributeName()
	{
		return attributeName;
	}

	AttributeType getAttributeType()
	{
		return attributeTypes.getValue();
	}
	
	interface TypeChangeCallback
	{
		void attributeTypeChanged(AttributeType newType);
	}
}
