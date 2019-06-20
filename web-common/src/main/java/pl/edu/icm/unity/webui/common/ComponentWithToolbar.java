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
	private AbstractOrderedLayout layout;
	
	public ComponentWithToolbar(Component main, Toolbar<?> toolbar)
	{
		this(main, toolbar, Alignment.TOP_RIGHT);
	}
	
	public ComponentWithToolbar(Component main, Toolbar<?> toolbar, Alignment toolbarAligment)
	{
		this.toolbar = toolbar;
		Orientation orientation = toolbar.getOrientation();
		layout = orientation == Orientation.HORIZONTAL ? 
				new VerticalLayout() : new HorizontalLayout();
		layout.setMargin(false);
		layout.setStyleName(Styles.toolbar.toString());
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
		layout.setComponentAlignment(toolbar, toolbarAligment);
		
		setSizeUndefined();
		setCompositionRoot(layout);
	}
	
	public void setToolbarVisible(boolean visible)
	{
		toolbar.setVisible(visible);
	}
	
	public void setSpacing (boolean spacing)
	{
		layout.setSpacing(spacing);
	}
}
