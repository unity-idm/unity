/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.authn.column;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

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
	private List<ComponentWithId> components = new ArrayList<>();
	
	public AuthnOptionsColumn(String title, float width)
	{
		this.title = title;
		this.width = width;
		init();
	}

	void addOptions(Collection<ComponentWithId> components)
	{
		for (ComponentWithId component: components)
		{
			authNOptions.addComponent(component.component);
			authNOptions.setComponentAlignment(component.component, Alignment.TOP_CENTER);
		}
		this.components.addAll(components);
	}
	
	void disableAllExcept(String exception)
	{
		for (ComponentWithId componentWithId: components)
		{
			if (!componentWithId.id.equals(exception))
			{
				componentWithId.component.setEnabled(false);
				if (componentWithId.component instanceof FirstFactorAuthNPanel)
				{
					FirstFactorAuthNPanel authNPanel = (FirstFactorAuthNPanel) componentWithId.component;
					authNPanel.cancel();
				}
			}
		}
	}

	void filter(String filter)
	{
		for (ComponentWithId componentWithId: components)
		{
			if (componentWithId.component instanceof AuthnsGridWidget)
			{
				AuthnsGridWidget grid = (AuthnsGridWidget) componentWithId.component;
				grid.filter(filter);
			}
		}
	}
	
	void enableAll()
	{
		for (ComponentWithId componentWithId: components)
			componentWithId.component.setEnabled(true);
	}
	
	int countAuthenticationOptions()
	{
		int ret = 0;
		for (ComponentWithId componentWithId: components)
			if (componentWithId.component instanceof FirstFactorAuthNPanel)
				ret++;
		return ret;
	}

	boolean hasGridWidget()
	{
		for (ComponentWithId componentWithId: components)
			if (componentWithId.component instanceof AuthnsGridWidget)
				return true;
		return false;
	}

	boolean focusFirst()
	{
		for (ComponentWithId componentWithId: components)
		{
			if (componentWithId.component instanceof AuthenticationUIController)
			{
				AuthenticationUIController authNPanel = (AuthenticationUIController) componentWithId.component;
				if (authNPanel.focusIfPossible())
					return true;
			}
		}
		return false;
	}

	FirstFactorAuthNPanel getAuthnOptionById(String id)
	{
		for (ComponentWithId componentWithId: components)
			if ((componentWithId.component instanceof FirstFactorAuthNPanel) 
					&& componentWithId.id.equals(id))
				return (FirstFactorAuthNPanel) componentWithId.component;
		return null;
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
	
	static class ComponentWithId
	{
		final String id;
		final Component component;

		ComponentWithId(String id, Component component)
		{
			this.id = id;
			this.component = component;
		}
	}
}
