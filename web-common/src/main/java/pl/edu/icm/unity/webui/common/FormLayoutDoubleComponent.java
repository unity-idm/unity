/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common;

import com.vaadin.ui.Component;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.HorizontalLayout;

/**
 * Intended to be used as {@link FormLayout} element (row). It holds 2 components. 
 * The first component is used as the primary FL component (with its caption on the left etc), 
 * while the other is displayed on its right (it can be a checkbox or edit button).
 * 
 * @author K. Benedyczak
 */
public class FormLayoutDoubleComponent extends HorizontalLayout
{
	public FormLayoutDoubleComponent(Component main, Component right)
	{
		setSpacing(true);
		addComponents(main, right);
		setExpandRatio(main, 1.0f);
		setCaption(main.getCaption());
		main.setCaption(null);
	}
}
