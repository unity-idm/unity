/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.webconsole.attribute;

import java.util.Collection;
import java.util.Collections;

import com.vaadin.ui.Label;

import pl.edu.icm.unity.base.attribute.AttributeType;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.webui.common.AttributeTypeUtils;
import pl.edu.icm.unity.webui.common.CompactFormLayout;
import pl.edu.icm.unity.webui.common.attributes.AttributeSelectionComboBox;
import pl.edu.icm.unity.webui.common.safehtml.HtmlSimplifiedLabel;

/**
 * Panel providing editing features of the attribute metadata. The panel
 * shows all aspects of an attribute except values.
 * If multiple attribute types are provided then it is possible to select the actual one.
 * Otherwise the selection is disabled and attribute type is fixed to the single one provided.
 * @author K. Benedyczak
 */
class AttributeMetaEditorPanel extends CompactFormLayout
{
	private MessageSource msg;
	
	private Label valueType;
	private Label typeDescription;
	
	private String attributeName;
	private Label cardinality;
	private Label unique;
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
		valueType = new Label(attributeType.getValueSyntax());
		valueType.setCaption(msg.getMessage("AttributeType.type"));
		addComponent(valueType);

		typeDescription = new HtmlSimplifiedLabel(attributeType.getDescription().getValue(msg));
		typeDescription.setCaption(msg.getMessage("AttributeType.description"));
		typeDescription.setWidth(100, Unit.PERCENTAGE);
		addComponent(typeDescription);
		
		Label group = new Label(groupPath);
		group.setCaption(msg.getMessage("Attribute.group"));
		group.setWidthFull();
		addComponent(group);
		
		cardinality = new Label();
		cardinality.setCaption(msg.getMessage("AttributeType.cardinality"));
		addComponent(cardinality);
		cardinality.setValue(AttributeTypeUtils.getBoundsDesc(msg, attributeType.getMinElements(), 
				attributeType.getMaxElements()));
		
		unique = new Label();
		unique.setCaption(msg.getMessage("AttributeType.uniqueValues"));
		addComponent(unique);
		unique.setValue(AttributeTypeUtils.getBooleanDesc(msg, attributeType.isUniqueValues()));

		setWidth(100, Unit.PERCENTAGE);
	}

	private void createAttributeWidget(String attributeName)
	{
		Label name = new Label(attributeName);
		name.setCaption(msg.getMessage("AttributeType.name"));
		addComponent(name);
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
			addComponent(this.attributeTypes);
			this.attributeTypes.setWidth(100, Unit.PERCENTAGE);
			this.attributeTypes.addSelectionListener(event -> changeAttribute());
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
		valueType.setValue(type.getValueSyntax());
		
		typeDescription.setValue(type.getDescription().getValue(msg));
		
		cardinality.setValue(AttributeTypeUtils.getBoundsDesc(msg, type.getMinElements(), 
				type.getMaxElements()));
		unique.setValue(AttributeTypeUtils.getBooleanDesc(msg, type.isUniqueValues()));
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
