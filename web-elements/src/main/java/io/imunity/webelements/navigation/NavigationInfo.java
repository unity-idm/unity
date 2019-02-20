/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webelements.navigation;

import org.springframework.beans.factory.ObjectFactory;

import com.vaadin.server.Resource;

import pl.edu.icm.unity.webui.common.Images;

/**
 * Contains information about @{link {@link UnityView}
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
	public final String caption;
	public final String shortCaption;
	public final Resource icon;
	public final int position;

	public NavigationInfo(NavigationInfoBuilder builder)

	{
		this.id = builder.id;
		this.parent = builder.parent;
		this.type = builder.type;
		this.objectFactory = builder.objectFactory;
		this.icon = builder.icon;
		this.caption = builder.caption;
		this.shortCaption = builder.shortCaption;
		this.position = builder.position;
	}

	public static class NavigationInfoBuilder
	{
		private String id;
		private NavigationInfo parent;
		private Type type;

		private ObjectFactory<?> objectFactory;
		private String caption;
		private String shortCaption;
		private Resource icon;
		private int position;

		public NavigationInfoBuilder(String id, Type type)
		{
			this.id = id;
			this.type = type;
			this.caption = id;
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

		public NavigationInfoBuilder withCaption(String caption)
		{
			this.caption = caption;
			return this;
		}
		
		public NavigationInfoBuilder withShortCaption(String caption)
		{
			this.shortCaption = caption;
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
