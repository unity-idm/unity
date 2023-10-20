/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.console.components;

import com.vaadin.componentfactory.Tooltip;
import com.vaadin.componentfactory.TooltipPosition;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.UI;

class HtmlTooltipAttach
{
	static void to(Component component, Html tooltipContent)
	{
		Tooltip tooltip = new Tooltip();
		tooltip.attachToComponent(component);
		tooltip.setPosition(TooltipPosition.BOTTOM);
		tooltip.add(tooltipContent);
		tooltip.setThemeName("light");
		UI.getCurrent().add(tooltip);
	}
}
