/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin23.elements;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.icon.VaadinIcon;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class MenuComponent
{
	public final Class<? extends Component> component;
	public final String tabName;
	public final VaadinIcon icon;
	public final List<Class<? extends Component>> subViews;

	private MenuComponent(Class<? extends Component> menuComponent, String tabName, VaadinIcon icon, List<Class<? extends Component>> subViews)
	{
		this.component = menuComponent;
		this.tabName = tabName;
		this.icon = icon;
		this.subViews = List.copyOf(subViews);
	}

	public static Builder builder(Class<? extends Component> component)
	{
		return new Builder(component);
	}

	public static final class Builder
	{
		private Class<? extends Component> component;
		private List<Class<? extends Component>> subViews = Collections.emptyList();
		private String tabName;
		private VaadinIcon icon;

		private Builder(Class<? extends Component> component)
		{
			this.component = component;
		}

		public Builder menu(Class<? extends Component> component)
		{
			this.component = component;
			return this;
		}

		public Builder tabName(String tabName)
		{
			this.tabName = tabName;
			return this;
		}

		public Builder icon(VaadinIcon icon)
		{
			this.icon = icon;
			return this;
		}

		@SafeVarargs
		public final Builder subViews(Class<? extends Component>... subViews)
		{
			this.subViews = Arrays.asList(subViews);
			return this;
		}

		public MenuComponent build()
		{
			return new MenuComponent(component, tabName, icon, subViews);
		}
	}

}
