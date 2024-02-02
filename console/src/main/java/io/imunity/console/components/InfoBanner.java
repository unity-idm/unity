/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.console.components;

import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import java.util.function.Function;

public class InfoBanner extends VerticalLayout
{
	public InfoBanner(Function<String, String> msg)
	{
		add(new H4(msg.apply("ViewWithSubViewBase.unsavedEdits")));
		setWidthFull();
		setAlignItems(FlexComponent.Alignment.CENTER);
		addClassName("u-unsaved-banner");
		setVisible(false);
	}
}
