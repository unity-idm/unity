/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.endpoint.common;

import com.vaadin.componentfactory.Tooltip;
import com.vaadin.componentfactory.TooltipPosition;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.UI;

public class HtmlTooltipAttacher
{
	public static Tooltip to(Component component, Html tooltipContent)
	{
		Tooltip tooltip = new Tooltip();
		tooltip.attachToComponent(component);
		tooltip.setPosition(TooltipPosition.BOTTOM);
		tooltip.add(tooltipContent);
		tooltip.setThemeName("light");
		UI.getCurrent().add(tooltip);
		return tooltip;
	}
	
	public static Tooltip to(Component component, String tooltipContent)
	{
		return to(component, new Html("<div>" + tooltipContent + "</div>"));
	}
}
