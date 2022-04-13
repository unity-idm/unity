/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole;

import com.vaadin.navigator.ViewChangeListener;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.HorizontalLayout;

import io.imunity.webelements.navigation.UnityViewWithSubViews;
import io.imunity.webelements.navigation.WarnComponent;

public class WebConsoleWarnViewComponent extends CustomComponent implements ViewChangeListener
{
	private HorizontalLayout main;

	public WebConsoleWarnViewComponent()
	{
		main = new HorizontalLayout();
		main.setWidth(100, Unit.PERCENTAGE);
		setCompositionRoot(main);
		setVisible(false);
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
		if (event.getNewView() instanceof UnityViewWithSubViews)
		{
			UnityViewWithSubViews view = (UnityViewWithSubViews) event.getNewView();
			WarnComponent warnComponent = view.getWarnComponent();
			warnComponent.addVisibilityChangeListener(v -> setVisible(v));
			main.addComponent(warnComponent);
		}
	}

}
