/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.elements;

import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import static io.imunity.vaadin.elements.CSSVars.SMALL_MARGIN;
import static io.imunity.vaadin.elements.VaadinClassNames.PANEL;

public class Panel extends VerticalLayout
{
	public Panel()
	{
		this(null);
	}
	
	
	public Panel(String header)
	{
		setMargin(true);
		setPadding(false);
		setSpacing(false);
		addClassName(PANEL.getName());
		if (header != null)
		{
			HorizontalLayout horizontalLayout = new HorizontalLayout();
			horizontalLayout.setWidthFull();
			Span label = new Span(header);
			label.getStyle()
					.set("margin", SMALL_MARGIN.value());
			horizontalLayout.add(label);
			horizontalLayout.getStyle()
			.set("background-color", "var(--unity-contrast)");
			add(horizontalLayout);
		}
		
		setSizeUndefined();
	}
}
