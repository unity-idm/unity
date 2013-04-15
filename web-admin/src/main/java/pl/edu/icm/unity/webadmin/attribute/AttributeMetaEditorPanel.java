/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.attribute;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.AttributeVisibility;
import pl.edu.icm.unity.webui.common.EnumComboBox;
import pl.edu.icm.unity.webui.common.MapComboBox;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.TextArea;

/**
 * Panel providing editing features of the attribute metadata. The panel
 * shows all aspects of an attribute except values. It is possible to edit 
 * attribute visibility if it makes sense for the type. 
 * If multiple attribute types are provided then it is possible to select the actual one.
 * Otherwise the selection is disabled and attribute type is fixed to the single one provided.
 * @author K. Benedyczak
 */
public class AttributeMetaEditorPanel extends FormLayout
{
	private UnityMessageSource msg;
	
	private Label valueType;
	private TextArea typeDescription;
	
	private String attributeName;
	private EnumComboBox<AttributeVisibility> visibility;
	private Label cardinality;
	private Label unique;
	private MapComboBox<AttributeType> attributeTypes;
	private TypeChangeCallback callback;

	public AttributeMetaEditorPanel(AttributeType attributeType, String groupPath, UnityMessageSource msg,
			AttributeVisibility visibility)
	{
		this(Collections.singletonList(attributeType), groupPath, msg);
		this.visibility.setEnumValue(visibility);
		typeDescription.setSizeUndefined();
		setSizeUndefined();
	}
	
	public AttributeMetaEditorPanel(Collection<AttributeType> attributeTypes, String groupPath, UnityMessageSource msg)
	{
		this.msg = msg;
		createAttributeSelectionWidget(attributeTypes);
		AttributeType selected = getAttributeType();
		initCommon(selected, groupPath, selected.getVisibility());
	}
	
	private void initCommon(AttributeType attributeType, String groupPath, AttributeVisibility attrVisibility)
	{
		valueType = new Label(attributeType.getValueType().getValueSyntaxId());
		valueType.setCaption(msg.getMessage("Attribute.type"));
		addComponent(valueType);

		typeDescription = new TextArea(msg.getMessage("Attribute.description"), 
				attributeType.getDescription());
		typeDescription.setReadOnly(true);
		typeDescription.setSizeFull();
		addComponent(typeDescription);
		
		Label group = new Label(groupPath);
		group.setCaption(msg.getMessage("Attribute.group"));
		addComponent(group);
		
		cardinality = new Label();
		cardinality.setCaption(msg.getMessage("Attribute.cardinality"));
		addComponent(cardinality);
		setCardinality(attributeType);
		
		unique = new Label();
		unique.setCaption(msg.getMessage("Attribute.uniqueValues"));
		addComponent(unique);
		setUnique(attributeType);

		visibility = new EnumComboBox<AttributeVisibility>(msg.getMessage("Attribute.visibility"), 
				msg, "AttributeVisibility.", 
				AttributeVisibility.class, attrVisibility);
		visibility.setWidth(10, Unit.EM);
		addComponent(visibility);
		setSizeFull();
	}

	private void createAttributeWidget(String attributeName)
	{
		Label name = new Label(attributeName);
		name.setCaption(msg.getMessage("Attribute.name"));
		addComponent(name);
	}
	
	private void createAttributeSelectionWidget(Collection<AttributeType> attributeTypes)
	{
		Map<String, AttributeType> typesByName = new HashMap<String, AttributeType>(attributeTypes.size());
		for (AttributeType at: attributeTypes)
			typesByName.put(at.getName(), at);
		this.attributeTypes = new MapComboBox<AttributeType>(msg.getMessage("Attribute.name"), typesByName, 
				typesByName.keySet().iterator().next());
		this.attributeTypes.setImmediate(true);
		
		if (attributeTypes.size() == 1)
		{
			createAttributeWidget(attributeTypes.iterator().next().getName());
		} else
		{
		
			addComponent(this.attributeTypes);
			this.attributeTypes.addValueChangeListener(new ValueChangeListener()
			{
				@Override
				public void valueChange(ValueChangeEvent event)
				{
					changeAttribute();
				}
			});
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

	private void setCardinality(AttributeType type)
	{
		String from = type.getMinElements()+"";
		String to = type.getMaxElements() == Integer.MAX_VALUE ? 
				msg.getMessage("Attribute.cardinalityNoLimit") : 
				type.getMaxElements()+"";
		String cardVal = "[" + from + ", " + to + "]";
		cardinality.setValue(cardVal);
	}
	
	private void setUnique(AttributeType type)
	{
		String val = type.isUniqueValues() ? msg.getMessage("yes") : msg.getMessage("no");
		unique.setValue(val);
	}
	
	private void changeAttribute()
	{
		AttributeType type = attributeTypes.getSelectedValue();
		valueType.setValue(type.getValueType().getValueSyntaxId());
		
		typeDescription.setReadOnly(false);
		typeDescription.setValue(type.getDescription());
		typeDescription.setReadOnly(true);
		
		visibility.setEnumValue(type.getVisibility());
		setCardinality(type);
		setUnique(type);
		if (callback != null)
			callback.attributeTypeChanged(type);
	}
	
	public String getAttributeName()
	{
		return attributeName;
	}

	public AttributeType getAttributeType()
	{
		return attributeTypes.getSelectedValue();
	}
	
	public AttributeVisibility getVisibility()
	{
		return visibility.getSelectedValue();
	}
	
	public interface TypeChangeCallback
	{
		public void attributeTypeChanged(AttributeType newType);
	}
}
