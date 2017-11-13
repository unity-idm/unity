/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common;

import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

import com.vaadin.event.Action;
import com.vaadin.event.Action.Handler;
import com.vaadin.server.Resource;

/**
 * Simplified {@link Handler} providing and handling a single action. 
 * In this implementation action is available for all targets. One can overwrite 
 * getActions() to change this.
 * 
 * @author K. Benedyczak
 */
public class SingleActionHandler2<T>
{		
	protected static final Action[] EMPTY = new Action[0];
	private boolean needsTarget = true;
	private boolean multiTarget = false;
	private boolean hideIfInactive = false;
	private Predicate<T> disabledPredicate;
	private Consumer<Set<T>> actionHandler;
	private String caption;
	private Resource icon;
	
	private SingleActionHandler2(String caption, Resource icon, Consumer<Set<T>> actionHandler)
	{
		this.caption = caption;
		this.icon = icon;
		this.actionHandler = actionHandler;
		disabledPredicate = a -> false;
	}
	
	public static <T> Builder<T> builder(String caption, Resource icon, Consumer<Set<T>> actionHandler)
	{
		return new Builder<>(caption, icon, actionHandler);
	}

	public static <T> Builder<T> builder(String caption, Resource icon, Class<T> clazz,
			Consumer<Set<T>> actionHandler)
	{
		return new Builder<>(caption, icon, actionHandler);
	}
	
	public static class Builder<T>
	{
		private SingleActionHandler2<T> obj;

		public Builder(String caption, Resource icon, Consumer<Set<T>> actionHandler)
		{
			this.obj = new SingleActionHandler2<>(caption, icon, actionHandler);
		}
		
		public Builder<T> dontRequireTarget()
		{
			this.obj.needsTarget = false;
			return this;
		}

		public Builder<T> multiTarget()
		{
			this.obj.multiTarget = true;
			return this;
		}

		public Builder<T> hideIfInactive()
		{
			this.obj.hideIfInactive = true;
			return this;
		}
		
		public Builder<T> withDisabledPredicate(Predicate<T> filter)
		{
			this.obj.disabledPredicate = filter;
			return this;
		}
		
		public SingleActionHandler2<T> build()
		{
			return obj;
		}
	}
	
	public boolean isNeedsTarget()
	{
		return needsTarget;
	}

	public boolean isMultiTarget()
	{
		return multiTarget;
	}
	
	public boolean isHideIfInactive()
	{
		return hideIfInactive;
	}

	public String getCaption()
	{
		return caption;
	}

	public Resource getIcon()
	{
		return icon;
	}

	public boolean isEnabled(Set<T> selection)
	{
		if (!multiTarget && needsTarget && selection.size() > 1)
			return false;
		if (needsTarget && selection.isEmpty())
			return false;
		if (selection.stream().anyMatch(disabledPredicate))
			return false;
		return true;
	}

	public boolean isVisible(Set<T> selection)
	{
		return hideIfInactive ? isEnabled(selection) : true;
	}
	
	public void handle(Set<T> selection)
	{
		actionHandler.accept(selection);
	}
}
