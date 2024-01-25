/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.endpoint.common;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;


/**
 * Component with a pair of subcomponents: a generic main component and a {@link Toolbar}.
 *  
 * @author P.Piernik
 */
public class ComponentWithToolbar extends VerticalLayout
{
	private final Toolbar<?> toolbar;
	
	public ComponentWithToolbar(Component main, Toolbar<?> toolbar)
	{
		this.toolbar = toolbar;
		setMargin(false);
		setSpacing(false);
		setPadding(false);
		add(toolbar);
		add(main);
	}
	
	public void setToolbarVisible(boolean visible)
	{
		toolbar.setVisible(visible);
	}
}
