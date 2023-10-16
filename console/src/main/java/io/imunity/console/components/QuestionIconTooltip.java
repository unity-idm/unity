/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.console.components;

import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;

public class QuestionIconTooltip
{
	public static Icon get(String tooltipText)
	{
		Icon icon = VaadinIcon.QUESTION_CIRCLE_O.create();
		icon.setTooltipText(tooltipText);
		icon.setClassName("field-icon-gap");
		return icon;
	}

	public static Icon getWithHtmlTooltip(String tooltipText)
	{
		Icon icon = VaadinIcon.QUESTION_CIRCLE_O.create();
		HtmlTooltip.forComponent(icon, new Html("<div>" + tooltipText + "</div>"));
		icon.setClassName("field-icon-gap");
		return icon;
	}
}
