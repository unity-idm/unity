/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webelements.menu.top;

import com.vaadin.server.Resource;
import com.vaadin.ui.TextField;

import io.imunity.webelements.menu.MenuElement;
import pl.edu.icm.unity.webui.common.Styles;

/**
 * Top menu text field. By default is collapsed. Expands on mouse hover
 * 
 * @author P.Piernik
 *
 */
public class TopMenuTextField extends TextField implements MenuElement
{

	public static TopMenuTextField get(Resource icon, String placeholder)
	{
		return new TopMenuTextField(icon, placeholder);
	}

	public TopMenuTextField(Resource icon, String placeholder)
	{
		setIcon(icon);
		setPlaceholder(placeholder);
		addStyleName(Styles.topMenuTextField.toString());
	}

	@Override
	public void setActive(boolean active)
	{
		setVisible(active);

	}

	@Override
	public String getMenuElementId()
	{
		return super.getId();
	}
}