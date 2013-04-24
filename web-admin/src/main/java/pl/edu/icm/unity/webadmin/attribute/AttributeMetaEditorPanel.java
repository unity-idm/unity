/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.attribute;

import java.util.Collection;
import java.util.Collections;
import java.util.SortedMap;
import java.util.TreeMap;

import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.AttributeVisibility;
import pl.edu.icm.unity.webui.common.AttributeTypeUtils;
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
		valueType.setCaption(msg.getMessage("AttributeType.type"));
		addComponent(valueType);

		typeDescription = new TextArea(msg.getMessage("AttributeType.description"), 
				attributeType.getDescription());
		typeDescription.setReadOnly(true);
		typeDescription.setSizeUndefined();
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

		visibility = new EnumComboBox<AttributeVisibility>(msg.getMessage("AttributeType.visibility"), 
				msg, "AttributeType.visibility.", 
				AttributeVisibility.class, attrVisibility);
		visibility.setSizeUndefined();
		visibility.setWidth(10, Unit.EM);
		addComponent(visibility);
		setSizeFull();
	}

	private void createAttributeWidget(String attributeName)
	{
		Label name = new Label(attributeName);
		name.setCaption(msg.getMessage("AttributeType.name"));
		addComponent(name);
	}
	
	private void createAttributeSelectionWidget(Collection<AttributeType> attributeTypes)
	{
		SortedMap<String, AttributeType> typesByName = new TreeMap<String, AttributeType>();
		for (AttributeType at: attributeTypes)
		{
			if (!at.isInstanceImmutable())
				typesByName.put(at.getName(), at);
		}
		String chosen = typesByName.size() > 0 ? typesByName.keySet().iterator().next() : null;
		this.attributeTypes = new MapComboBox<AttributeType>(msg.getMessage("AttributeType.name"), typesByName, 
				chosen);
		this.attributeTypes.setImmediate(true);
		this.attributeTypes.setSizeUndefined();
		this.attributeTypes.setWidth(100, Unit.PERCENTAGE);
		
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

	private void changeAttribute()
	{
		AttributeType type = attributeTypes.getSelectedValue();
		valueType.setValue(type.getValueType().getValueSyntaxId());
		
		typeDescription.setReadOnly(false);
		typeDescription.setValue(type.getDescription());
		typeDescription.setReadOnly(true);
		
		visibility.setEnumValue(type.getVisibility());
		cardinality.setValue(AttributeTypeUtils.getBoundsDesc(msg, type.getMinElements(), 
				type.getMaxElements()));
		unique.setValue(AttributeTypeUtils.getBooleanDesc(msg, type.isUniqueValues()));
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
