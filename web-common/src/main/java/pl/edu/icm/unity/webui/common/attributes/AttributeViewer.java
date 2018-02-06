/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.attributes;

import java.util.ArrayList;
import java.util.List;

import com.vaadin.ui.AbstractOrderedLayout;
import com.vaadin.ui.Component;
import com.vaadin.ui.Image;
import com.vaadin.ui.Label;
import com.vaadin.ui.AbstractField;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.webui.common.safehtml.HtmlConfigurableLabel;

/**
 * Shows an attribute values in read only mode. The look and feel is similar to the {@link FixedAttributeEditor}
 * but the value is shown in read only mode.
 * @author K. Benedyczak
 */
public class AttributeViewer
{
	private UnityMessageSource msg;
	private AttributeHandlerRegistry registry;
	private AttributeType attributeType;
	private Attribute attribute;
	private boolean showGroup;
	private List<Component> values;
	
	public AttributeViewer(UnityMessageSource msg, AttributeHandlerRegistry registry, 
			AttributeType attributeType, Attribute attribute, boolean showGroup)
	{
		this.msg = msg;
		this.registry = registry;
		this.attributeType = attributeType;
		this.attribute = attribute;
		this.showGroup = showGroup;
	}

	public void removeFromLayout(AbstractOrderedLayout parent)
	{
		for (Component c: values)
			parent.removeComponent(c);
	}

	public void addToLayout(AbstractOrderedLayout parent)
	{
		addToLayout(attributeType.getDisplayedName().getValue(msg), parent);
	}
	
	public List<Component> getAsComponents(String caption, String description)
	{
		createContents(caption, description);
		return values;
	}
	
	public void addToLayout(String caption, AbstractOrderedLayout parent)
	{
		I18nString description = attributeType.getDescription();
		String descriptionraw = description != null ? description.getValue(msg) : null;
		createContents(caption, descriptionraw);
		for (Component c: values)
			parent.addComponent(c);
	}
	
	private void createContents(String caption, String descriptionRaw)
	{
		if (showGroup && !attribute.getGroupPath().equals("/"))
			caption = caption + " @" + attribute.getGroupPath(); 

		int i = 1;
		values = new ArrayList<>();
		for (String o: attribute.getValues())
		{
			Component valueRepresentation = getRepresentation(o);
			String captionWithNum = (attribute.getValues().size() == 1) ? caption + ":" :
				caption + " (" + i + "):";
			valueRepresentation.setCaption(captionWithNum);
			if (descriptionRaw != null)
			{
				String descSafe = HtmlConfigurableLabel.conditionallyEscape(descriptionRaw);
				if (valueRepresentation instanceof AbstractField<?>)
					((AbstractField<?>)valueRepresentation).setDescription(descSafe);
				if (valueRepresentation instanceof Label)
					((Label)valueRepresentation).setDescription(descSafe);
				if (valueRepresentation instanceof Image)
					((Image)valueRepresentation).setDescription(descSafe);
			}
			
			valueRepresentation.addStyleName("u-baseline");
			values.add(valueRepresentation);
			i++;
		}
	}
	
	private Component getRepresentation(String value)
	{
		WebAttributeHandler handler = null;	
		if (attributeType == null)
			handler = registry.getHandlerWithStringFallback(attribute);
		else
			handler = registry.getHandlerWithStringFallback(attributeType);
		
		return handler.getRepresentation(value);
	}
}
