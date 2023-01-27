/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin23.endpoint.common.plugins.attributes;

import com.vaadin.flow.component.Component;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.webui.common.safehtml.HtmlConfigurableLabel;

import java.util.List;

public class AttributeViewer
{
	private MessageSource msg;
	private AttributeHandlerRegistryV23 registry;
	private AttributeType attributeType;
	private Attribute attribute;
	private boolean showGroup;
	private ComponentsGroup group;
	private AttributeViewerContext context;
	
	public AttributeViewer(MessageSource msg, AttributeHandlerRegistryV23 registry,
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
				valueRepresentation.getElement().setProperty("label", captionWithNum);
			}
			if (descriptionRaw != null)
			{
				String descSafe = HtmlConfigurableLabel.conditionallyEscape(descriptionRaw);
				valueRepresentation.getElement().setProperty("title", descSafe);
			}
			
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
