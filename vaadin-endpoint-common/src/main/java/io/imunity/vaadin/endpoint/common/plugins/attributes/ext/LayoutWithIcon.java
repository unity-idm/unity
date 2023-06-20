/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.endpoint.common.plugins.attributes.ext;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasLabel;
import com.vaadin.flow.component.HasStyle;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import io.imunity.vaadin.elements.InputLabel;

class LayoutWithIcon extends VerticalLayout implements HasLabel
{
	private final Label label;

	LayoutWithIcon(Component representation, Icon icon)
	{
		this.label = new InputLabel("");
		((HasStyle)representation).getStyle().set("width", "100%");
		HorizontalLayout layout = new HorizontalLayout(representation, icon);
		layout.setPadding(false);
		layout.setWidthFull();
		layout.getStyle().set("position", "relative");
		layout.setAlignItems(Alignment.CENTER);

		icon.getStyle().set("position", "absolute");
		icon.getStyle().set("left", "102%");

		add(label, layout);
		setPadding(false);
		setSpacing(false);
	}

	@Override
	public void setLabel(String label) {
		this.label.setText(label);
	}

	@Override
	public String getLabel() {
		return label.getText();
	}
}
