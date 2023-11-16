/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.elements;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;

import static io.imunity.vaadin.elements.VaadinClassNames.FIELD_ICON_GAP;

public class TooltipFactory
{
	public static Component get(String tooltipText)
	{
		Icon icon = VaadinIcon.QUESTION_CIRCLE_O.create();
		icon.setTooltipText(tooltipText);
		icon.setClassName(FIELD_ICON_GAP.getName());
		return icon;
	}
}
