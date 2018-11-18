/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.upman.common;

import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.HorizontalLayout;

/**
 * Component for displaying the unity view header
 * 
 * @author P.Piernik
 *
 */
public class ViewHeader extends CustomComponent implements ViewChangeListener
{
	private HorizontalLayout main;

	public ViewHeader()
	{
		main = new HorizontalLayout();
		main.setMargin(false);
		main.setSpacing(false);
		setCompositionRoot(main);
	}

	@Override
	public boolean beforeViewChange(ViewChangeEvent event)
	{
		return true;
	}

	@Override
	public void afterViewChange(ViewChangeEvent event)
	{
		main.removeAllComponents();
		View uView = event.getNewView();
		if (uView instanceof UpManView)
		{
			main.addComponent(((UpManView) uView).getViewHeader());
		}
	}

}
