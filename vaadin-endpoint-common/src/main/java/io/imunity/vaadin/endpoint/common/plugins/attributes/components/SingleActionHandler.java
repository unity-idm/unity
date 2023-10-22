/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.endpoint.common.plugins.attributes.components;

import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

import com.vaadin.flow.component.icon.VaadinIcon;

import pl.edu.icm.unity.base.message.MessageSource;

public class SingleActionHandler<T>
{		
	protected static final Consumer<?>[] EMPTY = new Consumer[0];
	private boolean needsTarget = true;
	private boolean multiTarget = false;
	private boolean hideIfInactive = false;
	private Predicate<T> disabledPredicate;
	private Predicate<Set<T>> disabledCompositePredicate;
	private Consumer<Set<T>> actionHandler;
	private String caption;
	private VaadinIcon icon;
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
	public static <T> Builder<T> builder4Add(MessageSource msg, Class<T> clazz)
	{
		return new Builder<T>()
				.withCaption(msg.getMessage("add"))
				.withIcon(VaadinIcon.PLUS_CIRCLE_O)
				.dontRequireTarget();
	}
	
	/**
	 * Doesn't require target, sets label and icon
	 * @param msg
	 * @param clazz
	 * @return
	 */
	public static <T> Builder<T> builder4Refresh(MessageSource msg, Class<T> clazz)
	{
		return new Builder<T>()
				.withCaption(msg.getMessage("refresh"))
				.withIcon(VaadinIcon.REFRESH)
				.dontRequireTarget();
	}

	/**
	 * Multitarget, sets icon and caption
	 * @param msg
	 * @param clazz
	 * @return
	 */
	public static <T> Builder<T> builder4Delete(MessageSource msg, Class<T> clazz)
	{
		return new Builder<T>()
				.withCaption(msg.getMessage("remove"))
				.withIcon(VaadinIcon.TRASH)
				.multiTarget();
	}

	/**
	 * Sets icon and caption
	 * @param msg
	 * @param clazz
	 * @return
	 */
	public static <T> Builder<T> builder4Edit(MessageSource msg, Class<T> clazz)
	{
		return new Builder<T>()
				.withCaption(msg.getMessage("edit"))
				.withIcon(VaadinIcon.EDIT);
	}
	
	/**
	 * Sets icon and caption
	 * @param msg
	 * @param clazz
	 * @return
	 */
	public static <T> Builder<T> builder4ShowDetails(MessageSource msg, Class<T> clazz)
	{
		return new Builder<T>()
				.withCaption(msg.getMessage("showDetails"))
				.withIcon(VaadinIcon.INFO);
	}
	
	/**
	 * Sets icon and caption
	 * @param msg
	 * @param clazz
	 * @return
	 */
	public static <T> Builder<T> builder4Copy(MessageSource msg, Class<T> clazz)
	{
		return new Builder<T>()
				.withCaption(msg.getMessage("copy"))
				.withIcon(VaadinIcon.COPY_O);
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

		public Builder<T> withIcon(VaadinIcon icon)
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

	public VaadinIcon getIcon()
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
