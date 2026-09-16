/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.endpoint.common;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.shared.Tooltip;
import com.vaadin.flow.component.shared.Tooltip.TooltipPosition;

public class HtmlTooltipAttacher
{
	public static Tooltip to(Component component, String tooltipContent)
	{
		Tooltip tooltip = Tooltip.forComponent(component);
		tooltip.setPosition(TooltipPosition.BOTTOM);
		tooltip.setMarkdown(tooltipContent);
		return tooltip;
	}
}
