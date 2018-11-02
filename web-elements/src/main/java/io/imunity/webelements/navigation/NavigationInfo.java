/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webelements.navigation;

import org.springframework.beans.factory.ObjectFactory;

import com.vaadin.server.Resource;

import pl.edu.icm.unity.webui.common.Images;

/**
 * 
 * @author P.Piernik
 *
 */
public class NavigationInfo
{
	public static enum Type
	{
		View, ViewGroup, ParameterizedView, DefaultView
	};

	public final String id;
	public final NavigationInfo parent;
	public final Type type;
	public final ObjectFactory<?> objectFactory;
	public final ViewDisplayNameProvider displayNameProvider;
	public final Resource icon;
	public final int position;

	public NavigationInfo(NavigationInfoBuilder builder)

	{
		this.id = builder.id;
		this.parent = builder.parent;
		this.type = builder.type;
		this.objectFactory = builder.objectFactory;
		this.icon = builder.icon;
		this.displayNameProvider = builder.displayNameProvider;
		this.position = builder.position;
	}

	//
	// public NavigationInfo(Type type, String id, NavigationInfo parent)
	// {
	// this(type, id, parent, null, e -> id, null, 0);
	// }
	//
	// public NavigationInfo(Type type, String id, NavigationInfo parent,
	// int order)
	// {
	// this(type, id, parent, null, e -> id, null, order);
	// }
	//
	// public NavigationInfo(Type type, String id, NavigationInfo parent,
	// ObjectFactory<?> objectFactory, ViewDisplayNameProvider
	// displayNameProvider)
	// {
	// this(type, id, parent, objectFactory, displayNameProvider, null, 0);
	// }
	//
	// public NavigationInfo(Type type, String id, NavigationInfo parent,
	// ViewDisplayNameProvider displayNameProvider, int order)
	// {
	// this(type, id, parent, null, displayNameProvider, null, order);
	// }
	//
	// public NavigationInfo(Type type, String id, NavigationInfo parent,
	// ObjectFactory<?> objectFactory, ViewDisplayNameProvider
	// displayNameProvider,
	// Resource icon, int order)
	// {
	// this.type = type;
	// this.id = id;
	// this.parent = parent;
	// this.objectFactory = objectFactory;
	// this.displayNameProvider = displayNameProvider;
	// this.icon = icon;
	// this.position = order;
	// }

	public static class NavigationInfoBuilder
	{
		private String id;
		private NavigationInfo parent;
		private Type type;

		private ObjectFactory<?> objectFactory;
		private ViewDisplayNameProvider displayNameProvider;
		private Resource icon;
		private int position;

		public NavigationInfoBuilder(String id, Type type)
		{
			this.id = id;
			this.type = type;
			this.displayNameProvider = e -> e.getViewName();
			this.position = 0;
			this.icon = Images.ok.getResource();
			this.parent = null;
		}

		public NavigationInfoBuilder withObjectFactory(ObjectFactory<?> objectFactory)
		{
			this.objectFactory = objectFactory;
			return this;
		}

		public NavigationInfoBuilder withPosition(int position)
		{
			this.position = position;
			return this;
		}

		public NavigationInfoBuilder withIcon(Resource icon)
		{
			this.icon = icon;
			return this;
		}

		public NavigationInfoBuilder withDisplayNameProvider(
				ViewDisplayNameProvider displayNameProvider)
		{
			this.displayNameProvider = displayNameProvider;
			return this;
		}

		public NavigationInfoBuilder withParent(NavigationInfo parent)
		{
			this.parent = parent;
			return this;
		}
		
		public NavigationInfo build()
		{
			return new NavigationInfo(this);
		}

	}

}
