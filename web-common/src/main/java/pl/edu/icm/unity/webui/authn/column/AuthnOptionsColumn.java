/*
 * Copyright (c) 2018 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.authn.column;

import java.util.Collection;

import com.vaadin.ui.Alignment;
import com.vaadin.ui.Component;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

/**
 * UI widget with column of authentication options.
 * 
 * @author K. Benedyczak
 */
class AuthnOptionsColumn extends CustomComponent
{
	private final String title;
	private final float width;
	
	private VerticalLayout authNOptions;
	
	public AuthnOptionsColumn(String title, float width)
	{
		this.title = title;
		this.width = width;
		init();
	}

	void addOptions(Collection<Component> components)
	{
		for (Component component: components)
		{
			authNOptions.addComponent(component);
			authNOptions.setComponentAlignment(component, Alignment.TOP_CENTER);
		}
	}
	
	private void init()
	{
		VerticalLayout column = new VerticalLayout();
		column.setMargin(false);
		setCompositionRoot(column);
		column.setWidth(width, Unit.EM);
		if (title != null)
		{
			Label title = new Label(this.title);
			column.addComponent(title);
			column.setComponentAlignment(title, Alignment.TOP_CENTER);
		}
		
		authNOptions = new VerticalLayout();
		authNOptions.setMargin(false);
		column.addComponent(authNOptions);
	}
}
