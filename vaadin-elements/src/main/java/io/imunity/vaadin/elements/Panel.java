/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.elements;

import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import static io.imunity.vaadin.elements.CSSVars.SMALL_MARGIN;
import static io.imunity.vaadin.elements.CssClassNames.PANEL;

public class Panel extends VerticalLayout
{
	public Panel()
	{
		this((String) null);
	}
	
	public Panel(String header)
	{
		setMargin(true);
		setPadding(false);
		setSpacing(false);
		addClassName(PANEL.getName());
		if (header != null)
		{
			HorizontalLayout headerLayout = new HorizontalLayout();
			headerLayout.setWidthFull();
			Span label = new Span(header);
			label.getStyle()
					.set("margin", SMALL_MARGIN.value());
			headerLayout.add(label);
			headerLayout.getStyle()
			.set("background-color", "var(--unity-contrast)");
			add(headerLayout);
		}
		
		setSizeUndefined();
	}
	
	public Panel(HorizontalLayout header)
	{
		setMargin(true);
		setPadding(false);
		setSpacing(false);
		addClassName(PANEL.getName());
		if (header != null)
		{
			add(header);
		}
		
		setSizeUndefined();
	}
}
