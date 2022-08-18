/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin23.elements;

import com.vaadin.componentfactory.Tooltip;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HtmlContainer;

public class TooltipAttacher
{
	public static void attachTooltip(String txt, Component target, HtmlContainer container)
	{
		Tooltip tooltip = new Tooltip();
		tooltip.attachToComponent(target);
		container.add(tooltip);
		tooltip.add(txt);
	}
}
