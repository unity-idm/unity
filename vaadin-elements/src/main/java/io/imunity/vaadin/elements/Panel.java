/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.elements;

import static io.imunity.vaadin.elements.CSSVars.SMALL_MARGIN;

import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

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
		addClassName(Styles.panel.toString());
		HorizontalLayout horizontalLayout = new HorizontalLayout();
		horizontalLayout.setWidthFull();
		if (header != null)
		{
			Span label = new Span(header);
			label.getStyle()
					.set("margin", SMALL_MARGIN.value());
			horizontalLayout.add(label);
		}
		horizontalLayout.getStyle()
				.set("background-color", "var(--contrast)");
		add(horizontalLayout);
		setSizeUndefined();
	}
}
