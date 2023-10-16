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

public class HtmlTooltip
{
	public static void forComponent(Component attached, Html component)
	{
		Tooltip tooltip = new Tooltip();
		tooltip.attachToComponent(attached);
		tooltip.setPosition(TooltipPosition.BOTTOM);
		tooltip.add(component);
		tooltip.setThemeName("light");
		UI.getCurrent().add(tooltip);
	}
}
