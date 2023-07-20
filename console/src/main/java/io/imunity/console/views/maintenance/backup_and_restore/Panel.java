/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.console.views.maintenance.backup_and_restore;

import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

class Panel extends VerticalLayout
{
	public Panel(String header)
	{
		setMargin(true);
		setPadding(false);
		setSpacing(false);
		getStyle().set("box-shadow", "0 2px 3px rgba(0, 0, 0, 0.05)");
		HorizontalLayout horizontalLayout = new HorizontalLayout();
		horizontalLayout.setWidthFull();
		Label label = new Label(header);
		label.getStyle().set("margin", "var(--small-margin)");
		horizontalLayout.add(label);
		horizontalLayout.getStyle().set("background-color", "var(--lumo-contrast-5pct)");
		add(horizontalLayout);
		setSizeUndefined();
	}
}
