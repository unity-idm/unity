/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.attributes;

import java.util.ArrayList;
import java.util.List;

import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.AttributeVisibility;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.ListOfEmbeddedElements;

/**
 * Attribute editor allowing to edit a fixed attribute. It can show the (also fixed) group 
 * or not.
 * 
 * @author K. Benedyczak
 */
public class FixedAttributeEditor extends AbstractAttributeEditor
{
	private AttributeType attributeType;
	private String caption;
	private String description;
	private String group;
	private boolean showGroup;
	private ListOfEmbeddedElements<LabelledValue> valuesComponent;
	private AttributeVisibility visibility;
	private VerticalLayout main = new VerticalLayout();

	public FixedAttributeEditor(UnityMessageSource msg, AttributeHandlerRegistry registry, 
			AttributeType attributeType, boolean showGroup, String group, AttributeVisibility visibility,
			String caption, String description)
	{
		super(msg, registry);
		this.attributeType = attributeType;
		this.showGroup = showGroup;
		this.group = group;
		this.visibility = visibility;
		this.caption = caption;
		this.description = description;
		initUI();
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public Attribute<?> getAttribute() throws FormValidationException
	{
		List<LabelledValue> values = valuesComponent.getElements();
		List<Object> aValues = new ArrayList<>(values.size());
		for (LabelledValue v: values)
			aValues.add(v.getValue());
		return new Attribute(attributeType.getName(), attributeType.getValueType(), group, visibility, aValues);
	}
	
	private void initUI()
	{
		if (caption == null)
			caption = attributeType.getName();
		if (description == null)
			description = attributeType.getDescription();
		main.setSpacing(true);
		
		if (showGroup)
		{
			main.addComponent(new Label(group));
		}

		valuesComponent = getValuesPart(attributeType, caption);
		main.addComponent(valuesComponent);
		
		setCompositionRoot(main);
	}
}
