/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.console.views.maintenance.backup_and_restore;

import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import static io.imunity.vaadin.elements.CSSVars.SMALL_MARGIN;

class Panel extends VerticalLayout
{
	Panel(String header)
	{
		setMargin(true);
		setPadding(false);
		setSpacing(false);
		getStyle().set("box-shadow", "0 2px 3px rgba(0, 0, 0, 0.05)");
		HorizontalLayout horizontalLayout = new HorizontalLayout();
		horizontalLayout.setWidthFull();
		Span label = new Span(header);
		label.getStyle().set("margin", SMALL_MARGIN.value());
		horizontalLayout.add(label);
		horizontalLayout.getStyle().set("background-color", "var(--contrast)");
		add(horizontalLayout);
		setSizeUndefined();
	}
}
