/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.attributes;

import java.util.ArrayList;
import java.util.List;

import com.vaadin.ui.AbstractOrderedLayout;
import com.vaadin.ui.Label;

import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.AttributeVisibility;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.ListOfEmbeddedElementsStub;

/**
 * Attribute editor allowing to edit a fixed attribute type. It can show the (also fixed) group 
 * or not. The initial values can be optionally set.
 * <p>
 * This class is not a component on its own - instead it can be added to a parent container.
 * 
 * @author K. Benedyczak
 */
public class FixedAttributeEditor extends AbstractAttributeEditor
{
	private AttributeType attributeType;
	private String caption;
	private String description;
	private String group;
	private Label groupLabel;
	private boolean showGroup;
	private ListOfEmbeddedElementsStub<LabelledValue> valuesComponent;
	private AttributeVisibility visibility;
	private boolean required;

	public FixedAttributeEditor(UnityMessageSource msg, AttributeHandlerRegistry registry, 
			AttributeType attributeType, boolean showGroup, String group, AttributeVisibility visibility,
			String caption, String description, boolean required, AbstractOrderedLayout parent)
	{
		super(msg, registry);
		this.attributeType = attributeType;
		this.showGroup = showGroup;
		this.group = group;
		this.visibility = visibility;
		this.caption = caption;
		this.description = description;
		this.required = required;
		initUI(parent);
	}
	
	public void setAttributeValues(List<?> values)
	{
		List<LabelledValue> labelledValues = new ArrayList<>(values.size());
		for (Object value: values)
			labelledValues.add(new LabelledValue(value, caption));
		valuesComponent.setEntries(labelledValues);
	}
	
	public AttributeType getAttributeType()
	{
		return attributeType;
	}

	public String getGroup()
	{
		return group;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public Attribute<?> getAttribute() throws FormValidationException
	{
		List<LabelledValue> values = valuesComponent.getElements();
		List<Object> aValues = new ArrayList<>(values.size());
		boolean allNull = true;
		for (LabelledValue v: values)
		{
			aValues.add(v.getValue());
			if (v.getValue() != null)
				allNull = false;
		}
		
		return allNull ? null : 
			new Attribute(attributeType.getName(), attributeType.getValueType(), group, visibility, aValues);
	}
	
	private void initUI(AbstractOrderedLayout parent)
	{
		if (caption == null)
			caption = attributeType.getName() + ":";
		if (description == null)
			description = attributeType.getDescription();
		
		if (showGroup)
		{
			groupLabel = new Label(msg.getMessage("Attributes.groupOfAttribute", group));
			parent.addComponent(groupLabel);
		}

		valuesComponent = getValuesPart(attributeType, caption, required, parent);
	}
}
