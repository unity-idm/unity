/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */


package io.imunity.webconsole.layout;

import com.vaadin.ui.HorizontalLayout;

import pl.edu.icm.unity.webui.common.Styles;

/**
 * Top menu bar
 * 
 * @author P.Piernik
 *
 */
public class TopMenu extends HorizontalLayout
{
	public TopMenu()
	{
		setHeight(0, Unit.PIXELS);
		setStyleName(Styles.topMenu.toString());
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