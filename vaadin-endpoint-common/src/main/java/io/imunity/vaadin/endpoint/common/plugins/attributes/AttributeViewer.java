/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.endpoint.common.plugins.attributes;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasLabel;

import io.imunity.vaadin.endpoint.common.HtmlTooltipAttacher;
import io.imunity.vaadin.endpoint.common.safe_html.HtmlConfigurableLabel;
import pl.edu.icm.unity.base.attribute.Attribute;
import pl.edu.icm.unity.base.attribute.AttributeType;
import pl.edu.icm.unity.base.i18n.I18nString;
import pl.edu.icm.unity.base.message.MessageSource;

import java.util.List;

public class AttributeViewer
{
	private final MessageSource msg;
	private final AttributeHandlerRegistry registry;
	private final AttributeType attributeType;
	private final Attribute attribute;
	private final LabelContext labelContext;
	private final ComponentsGroup group;
	private final AttributeViewerContext context;
	
	public AttributeViewer(MessageSource msg, AttributeHandlerRegistry registry,
	                       AttributeType attributeType, Attribute attribute, LabelContext labelContext,
	                       AttributeViewerContext context)
	{
		this.msg = msg;
		this.registry = registry;
		this.attributeType = attributeType;
		this.attribute = attribute;
		this.labelContext = labelContext;
		this.group = new ComponentsGroup();
		this.context = context;
		generate();
	}
	
	public ComponentsGroup getComponentsGroup()
	{
		return group;
	}
	
	public void clear()
	{
		group.removeAll();
	}
	
	public List<Component> getAsComponents(String description)
	{
		createContents(description);
		return group.getComponents();
	}
	
	private void generate()
	{
		I18nString description = attributeType.getDescription();
		String descriptionraw = description != null ? description.getValue(msg) : null;
		createContents(descriptionraw);
	}

	private void createContents(String description)
	{
		group.removeAll();
		List<Component> components = attribute.getValues().stream()
				.map(value ->
				{
					Component representation = getRepresentation(value);
					if(description != null && (attributeType.getDisplayedName() == null || !description.equals(attributeType.getDisplayedName().getValue(msg))))
					{
						String descSafe = HtmlConfigurableLabel.conditionallyEscape(description);
						HtmlTooltipAttacher.to(representation, descSafe);
					}
					return representation;
				}).toList();
		components.stream().findFirst().ifPresent(component -> ((HasLabel)component).setLabel(labelContext.getLabel()));
		components.forEach(group::addComponent);
	}

	private Component getRepresentation(String value)
	{
		WebAttributeHandler handler;
		if (attributeType == null)
			handler = registry.getHandlerWithStringFallback(attribute);
		else
			handler = registry.getHandlerWithStringFallback(attributeType);
		
		return handler.getRepresentation(value, context);
	}
}
