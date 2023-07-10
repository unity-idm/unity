/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.elements;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.icon.VaadinIcon;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class MenuComponent
{
	public final Class<? extends UnityViewComponent> component;
	public final String tabName;
	public final VaadinIcon icon;
	public final List<Class<? extends Component>> subViews;
	public final List<MenuComponent> subTabs;

	private MenuComponent(Class<? extends UnityViewComponent> menuComponent, String tabName, VaadinIcon icon,
						  List<Class<? extends Component>> subViews,
						  List<MenuComponent> subTabs)
	{
		this.component = menuComponent;
		this.tabName = tabName;
		this.icon = icon;
		this.subViews = List.copyOf(subViews);
		this.subTabs = List.copyOf(subTabs);
	}

	public static Builder builder(Class<? extends UnityViewComponent> component)
	{
		return new Builder(component);
	}

	public static Builder builder(MenuComponent... subTabs)
	{
		return new Builder(subTabs);
	}

	public static final class Builder
	{
		private Class<? extends UnityViewComponent> component;
		private List<Class<? extends Component>> subViews = Collections.emptyList();
		private String tabName;
		private VaadinIcon icon;
		private List<MenuComponent> subTabs = Collections.emptyList();

		private Builder(Class<? extends UnityViewComponent> component)
		{
			this.component = component;
		}

		private Builder(MenuComponent... subTabs)
		{
			subTabs(subTabs);
		}

		public Builder menu(Class<? extends UnityViewComponent> component)
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

		public Builder subTabs(MenuComponent... subTabs)
		{
			this.subTabs = Arrays.asList(subTabs);
			return this;
		}

		public MenuComponent build()
		{
			return new MenuComponent(component, tabName, icon, subViews, subTabs);
		}
	}

}
