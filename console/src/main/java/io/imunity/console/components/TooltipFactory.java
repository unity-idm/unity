/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.console.components;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;

public class TooltipFactory
{
	public static Component get(String tooltipText)
	{
		Icon icon = VaadinIcon.QUESTION_CIRCLE_O.create();
		icon.setTooltipText(tooltipText);
		icon.setClassName("field-icon-gap");
		return icon;
	}

	public static Component getWithHtmlContent(String tooltipText)
	{
		Icon icon = VaadinIcon.QUESTION_CIRCLE_O.create();
		HtmlTooltipAttach.to(icon, new Html("<div>" + tooltipText + "</div>"));
		icon.setClassName("field-icon-gap");
		return icon;
	}
}
