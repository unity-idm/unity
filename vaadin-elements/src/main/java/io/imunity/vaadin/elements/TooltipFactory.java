/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.elements;

import static io.imunity.vaadin.elements.CssClassNames.FIELD_ICON_GAP;

import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;

public class TooltipFactory
{
	public static Icon get(String tooltipText)
	{
		Icon icon = VaadinIcon.QUESTION_CIRCLE_O.create();
		icon.setTooltipText(tooltipText);
		icon.setClassName(FIELD_ICON_GAP.getName());
		return icon;
	}
}
