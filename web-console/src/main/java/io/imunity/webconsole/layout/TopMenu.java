/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */


package io.imunity.webconsole.layout;

import com.vaadin.ui.HorizontalLayout;

/**
 * Top menu bar
 * 
 * @author P.Piernik
 *
 */
public class TopMenu extends HorizontalLayout
{

	public static final String STYLE_NAME = "topMenu";

	public TopMenu()
	{
		super();
		setHeight(0, Unit.PIXELS);
		setStyleName(STYLE_NAME);
		setMargin(false);
		setSpacing(false);
	}

	public <C extends MenuComponent<?>> C add(C c)
	{
		if (c.getCaption() != null && !c.getCaption().trim().isEmpty()) {
			c.setCaption(null);
		}
		addComponent(c);
		return c;
	}
}