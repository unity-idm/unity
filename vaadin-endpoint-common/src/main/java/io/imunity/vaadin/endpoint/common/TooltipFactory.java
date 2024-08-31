/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.endpoint.common;

import static io.imunity.vaadin.elements.CssClassNames.FIELD_ICON_GAP;

import com.vaadin.componentfactory.Tooltip;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;

import io.imunity.vaadin.endpoint.common.api.HtmlTooltipFactory;


@org.springframework.stereotype.Component
public class TooltipFactory implements HtmlTooltipFactory
{
	public static ComponentWithTooltip getWithHtmlContent(String tooltipText)
	{
		Icon icon = VaadinIcon.QUESTION_CIRCLE_O.create();
		Tooltip tooltip = HtmlTooltipAttacher.to(icon, tooltipText);
		icon.setClassName(FIELD_ICON_GAP.getName());
		return new ComponentWithTooltip(icon, tooltip);
	}

	@Override
	public Component get(String tooltipText)
	{
		return getWithHtmlContent(tooltipText).component;
	}
	
	public static record ComponentWithTooltip(Component component, Tooltip tooltip) {}
	
}
