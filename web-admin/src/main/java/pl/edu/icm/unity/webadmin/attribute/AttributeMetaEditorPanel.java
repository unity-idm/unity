/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.attribute;

import java.util.Collection;
import java.util.Collections;

import com.vaadin.ui.Label;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.types.basic.AttributeType;
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
public class AttributeMetaEditorPanel extends CompactFormLayout
{
	private UnityMessageSource msg;
	
	private Label valueType;
	private Label typeDescription;
	
	private String attributeName;
	private Label cardinality;
	private Label unique;
	private AttributeSelectionComboBox attributeTypes;
	private TypeChangeCallback callback;

	public AttributeMetaEditorPanel(AttributeType attributeType, String groupPath, UnityMessageSource msg)
	{
		this(Collections.singletonList(attributeType), groupPath, msg);
	}
	
	public AttributeMetaEditorPanel(Collection<AttributeType> attributeTypes, String groupPath, 
			UnityMessageSource msg)
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
	
	public TypeChangeCallback getCallback()
	{
		return callback;
	}

	public void setCallback(TypeChangeCallback callback)
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

	public void setAttributeType(String name)
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
	
	public String getAttributeName()
	{
		return attributeName;
	}

	public AttributeType getAttributeType()
	{
		return attributeTypes.getValue();
	}
	
	public interface TypeChangeCallback
	{
		public void attributeTypeChanged(AttributeType newType);
	}
}
