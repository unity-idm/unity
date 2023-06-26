/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.auth;

import com.vaadin.flow.component.*;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * UI widget with column of authentication options.
 */
@Tag("div")
class AuthnOptionsColumn extends Component implements HasComponents, HasStyle
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
			authNOptions.add(component.component);
			authNOptions.setAlignItems(FlexComponent.Alignment.CENTER);
		}
		this.components.addAll(components);
	}
	
	void disableAllExcept(String exception)
	{
		for (ComponentWithId componentWithId: components)
		{
			if (!componentWithId.id.equals(exception))
			{
				((HasEnabled)componentWithId.component).setEnabled(false);
				if (componentWithId.component instanceof FirstFactorAuthNPanel authNPanel)
				{
					authNPanel.cancel();
					((HasEnabled)componentWithId.component).setEnabled(false);
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
			((HasEnabled)componentWithId.component).setEnabled(true);
	}
	
	int countAuthenticationOptions()
	{
		int ret = 0;
		for (ComponentWithId componentWithId: components)
			ret += componentWithId.authNItemsCount;
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
		{
			Optional<FirstFactorAuthNPanel> authnOption = componentWithId.getAuthnOptionById(id);
			if (authnOption.isPresent())
				return authnOption.get();
		}
		return null;
	}

	
	private void init()
	{
		VerticalLayout column = new VerticalLayout();
		column.setMargin(false);
		column.setPadding(false);
		add(column);
		column.setWidth(width, Unit.EM);
		if (title != null)
		{
			Label title = new Label(this.title);
			title.addClassName("u-authn-columnTitle");
			column.add(title);
			column.setAlignItems(FlexComponent.Alignment.CENTER);
		}
		
		authNOptions = new VerticalLayout();
		authNOptions.setMargin(false);
		authNOptions.setPadding(false);
		authNOptions.getStyle().set("gap", "0");
		column.add(authNOptions);
	}
	
	static class ComponentWithId
	{
		final String id;
		final Component component;
		final int authNItemsCount;
		private final Function<String, Optional<FirstFactorAuthNPanel>> optionFinder;

		ComponentWithId(String id, Component component, int authNItemsCount, 
				Function<String, Optional<FirstFactorAuthNPanel>> optionFinder)
		{
			this.id = id;
			this.component = component;
			this.authNItemsCount = authNItemsCount;
			this.optionFinder = optionFinder;
		}
		
		static ComponentWithId createNonLoginComponent(String id, Component component)
		{
			return new ComponentWithId(id, component, 0, optid -> Optional.empty());
		}

		static ComponentWithId createSimpleLoginComponent(String id, FirstFactorAuthNPanel component)
		{
			return new ComponentWithId(id, component, 1, 
					optid -> Optional.ofNullable(id.equals(optid) ? component : null));
		}
		
		Optional<FirstFactorAuthNPanel> getAuthnOptionById(String id)
		{
			return optionFinder.apply(id);
		}
	}
}
