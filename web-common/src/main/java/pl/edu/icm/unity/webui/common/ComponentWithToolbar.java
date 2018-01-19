/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common;

import com.vaadin.shared.ui.Orientation;
import com.vaadin.ui.AbstractOrderedLayout;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Component;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.VerticalLayout;

/**
 * Component with a pair of subcomponents: a generic main component and a {@link Toolbar}.
 * 
 * The horizontal toolbar is placed over the main component on the right, the vertical toolabr on the right side 
 * of the main component.
 *  
 * @author K. Benedyczak
 */
public class ComponentWithToolbar extends CustomComponent
{
	private Toolbar<?> toolbar;
	
	public ComponentWithToolbar(Component main, Toolbar<?> toolbar)
	{
		this.toolbar = toolbar;
		Orientation orientation = toolbar.getOrientation();
		AbstractOrderedLayout layout = orientation == Orientation.HORIZONTAL ? 
				new VerticalLayout() : new HorizontalLayout();
		layout.setMargin(false);
		if (orientation == Orientation.HORIZONTAL)
		{
			layout.addComponent(toolbar);
			layout.addComponent(main);
		} else
		{
			layout.addComponent(main);
			layout.addComponent(toolbar);
		}
		layout.setSizeFull();
		layout.setExpandRatio(main, 1.0f);
		layout.setComponentAlignment(toolbar, Alignment.TOP_RIGHT);
		main.setSizeFull();
		
		setSizeUndefined();
		setCompositionRoot(layout);
	}
	
	public void setToolbarVisible(boolean visible)
	{
		toolbar.setVisible(visible);
	}	
}
