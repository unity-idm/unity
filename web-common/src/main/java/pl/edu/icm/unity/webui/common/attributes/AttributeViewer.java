/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.attributes;

import java.util.List;

import com.vaadin.ui.AbstractField;
import com.vaadin.ui.Component;
import com.vaadin.ui.Image;
import com.vaadin.ui.Label;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.webui.common.attributes.edit.FixedAttributeEditor;
import pl.edu.icm.unity.webui.common.composite.ComponentsGroup;
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
	private ComponentsGroup group;
	private AttributeViewerContext context;
	
	public AttributeViewer(UnityMessageSource msg, AttributeHandlerRegistry registry, 
			AttributeType attributeType, Attribute attribute, boolean showGroup, 
			AttributeViewerContext context)
	{
		this.msg = msg;
		this.registry = registry;
		this.attributeType = attributeType;
		this.attribute = attribute;
		this.showGroup = showGroup;
		this.group = new ComponentsGroup();
		this.context = context;
		generate(attributeType.getDisplayedName().getValue(msg));
	}
	
	public ComponentsGroup getComponentsGroup()
	{
		return group;
	}
	
	public void clear()
	{
		group.removeAll();
	}
	
	public List<Component> getAsComponents(String caption, String description)
	{
		createContents(caption, description);
		return group.getComponents();
	}
	
	private void generate(String caption)
	{
		I18nString description = attributeType.getDescription();
		String descriptionraw = description != null ? description.getValue(msg) : null;
		createContents(caption, descriptionraw);
	}
	
	private void createContents(String caption, String descriptionRaw)
	{
		if (showGroup && !attribute.getGroupPath().equals("/"))
			caption = caption + " @" + attribute.getGroupPath(); 

		int i = 1;
		group.removeAll();
		for (String o: attribute.getValues())
		{
			Component valueRepresentation = getRepresentation(o);
			if (context.isShowCaption())
			{
				String captionWithNum = (attribute.getValues().size() == 1) ? caption + ":" :
					caption + " (" + i + "):";
				valueRepresentation.setCaption(captionWithNum);
			}
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
			group.addComponent(valueRepresentation);
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
		
		return handler.getRepresentation(value, context);
	}
}
