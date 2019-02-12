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

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;

/**
 * Simplified {@link Handler} providing and handling a single action. 
 * In this implementation action is available for all targets. One can overwrite 
 * getActions() to change this.
 * 
 * @author K. Benedyczak
 */
public class SingleActionHandler<T>
{		
	protected static final Action[] EMPTY = new Action[0];
	private boolean needsTarget = true;
	private boolean multiTarget = false;
	private boolean hideIfInactive = false;
	private Predicate<T> disabledPredicate;
	private Predicate<Set<T>> disabledCompositePredicate;
	private Consumer<Set<T>> actionHandler;
	private String caption;
	private Resource icon;
	private boolean forceDisabled = false;
	
	private SingleActionHandler()
	{
		disabledPredicate = a -> false;
		disabledCompositePredicate = a -> false;
	}
	
	public static <T> Builder<T> builder()
	{
		return new Builder<>();
	}

	public static <T> Builder<T> builder(Class<T> clazz)
	{
		return new Builder<>();
	}

	/**
	 * Doesn't require target, sets icon and caption.
	 * @param msg
	 * @param clazz
	 * @return
	 */
	public static <T> Builder<T> builder4Add(UnityMessageSource msg, Class<T> clazz)
	{
		return new Builder<T>()
				.withCaption(msg.getMessage("add"))
				.withIcon(Images.add.getResource())
				.dontRequireTarget();
	}
	
	/**
	 * Doesn't require target, sets label and icon
	 * @param msg
	 * @param clazz
	 * @return
	 */
	public static <T> Builder<T> builder4Refresh(UnityMessageSource msg, Class<T> clazz)
	{
		return new Builder<T>()
				.withCaption(msg.getMessage("refresh"))
				.withIcon(Images.refresh.getResource())
				.dontRequireTarget();
	}

	/**
	 * Multitarget, sets icon and caption
	 * @param msg
	 * @param clazz
	 * @return
	 */
	public static <T> Builder<T> builder4Delete(UnityMessageSource msg, Class<T> clazz)
	{
		return new Builder<T>()
				.withCaption(msg.getMessage("remove"))
				.withIcon(Images.delete.getResource())
				.multiTarget();
	}

	/**
	 * Sets icon and caption
	 * @param msg
	 * @param clazz
	 * @return
	 */
	public static <T> Builder<T> builder4Edit(UnityMessageSource msg, Class<T> clazz)
	{
		return new Builder<T>()
				.withCaption(msg.getMessage("edit"))
				.withIcon(Images.edit.getResource());
	}
	
	/**
	 * Sets icon and caption
	 * @param msg
	 * @param clazz
	 * @return
	 */
	public static <T> Builder<T> builder4ShowDetails(UnityMessageSource msg, Class<T> clazz)
	{
		return new Builder<T>()
				.withCaption(msg.getMessage("showDetails"))
				.withIcon(Images.info.getResource());
	}
	
	/**
	 * Sets icon and caption
	 * @param msg
	 * @param clazz
	 * @return
	 */
	public static <T> Builder<T> builder4Copy(UnityMessageSource msg, Class<T> clazz)
	{
		return new Builder<T>()
				.withCaption(msg.getMessage("copy"))
				.withIcon(Images.copy.getResource());
	}
	
	public static class Builder<T>
	{
		private SingleActionHandler<T> obj;

		public Builder()
		{
			this.obj = new SingleActionHandler<>();
		}

		public Builder<T> withCaption(String caption)
		{
			this.obj.caption = caption;
			return this;
		}

		public Builder<T> withIcon(Resource icon)
		{
			this.obj.icon = icon;
			return this;
		}

		public Builder<T> withHandler(Consumer<Set<T>> actionHandler)
		{
			this.obj.actionHandler = actionHandler;
			return this;
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

		public Builder<T> withDisabledCompositePredicate(Predicate<Set<T>> filter)
		{
			this.obj.disabledCompositePredicate = filter;
			return this;
		}
		
		public SingleActionHandler<T> build()
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
		if (disabledCompositePredicate.test(selection))
			return false;
		if (forceDisabled)
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
	
	public void setDisabled(boolean disabled)
	{
		this.forceDisabled = disabled;
	}
	
	public void setHideIfInactive(boolean hideIfInactive)
	{
		this.hideIfInactive = hideIfInactive;
	}
}
