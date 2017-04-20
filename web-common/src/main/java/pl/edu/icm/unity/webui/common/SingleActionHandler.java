/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common;

import java.util.ArrayList;
import java.util.Collection;

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
@SuppressWarnings("serial")
public abstract class SingleActionHandler implements Handler
{		
	protected static final Action[] EMPTY = new Action[0];
	private Action[] action;
	private boolean needsTarget = true;
	private boolean multiTarget = false;
	private boolean hideIfNotNeeded = false;
	protected ActionButtonCallback callback;
	
	public SingleActionHandler(String caption, Resource icon)
	{
		Action a = new Action(caption, icon);
		action = new Action[] {a};
	}
		
	public boolean isNeedsTarget()
	{
		return needsTarget;
	}

	public void setNeedsTarget(boolean needsTarget)
	{
		this.needsTarget = needsTarget;
	}
	
	public boolean isMultiTarget()
	{
		return multiTarget;
	}
	
	public void setMultiTarget(boolean multiTarget)
	{
		this.multiTarget = multiTarget;
	}

	public boolean isHideIfNotNeeded()
	{
		return hideIfNotNeeded;
	}

	public void setHideIfNotNeeded(boolean hideIfNotNeed)
	{
		this.hideIfNotNeeded = hideIfNotNeed;
	}
	
	public Action getActionUnconditionally()
	{
		return action[0];
	}
	
	@Override
	public Action[] getActions(Object target, Object sender)
	{
		if (target == null)
		{
			if (needsTarget)
				return EMPTY;

		} else
		{
			if (target instanceof Collection<?>)
			{
			    Collection<?> t = (Collection<?>) target;
			    if (t.size() > 1 && !multiTarget && needsTarget)
			        return EMPTY;
			    if (t.isEmpty() && needsTarget)
			        return EMPTY;       
			} 
		}	
		return action;
	}
	
	@Override
	public void handleAction(Action action, Object sender, Object target)
	{
		if (action != this.action[0])
			return;
		
		Object wrTarget = target;
		
		if (target != null)
		{
			if (multiTarget && !(target instanceof Collection<?>))
			{
				ArrayList<Object> ntarget = new ArrayList<Object>();
				ntarget.add(target);
				wrTarget = ntarget;
			}
			if (!multiTarget && (target instanceof Collection<?>))
			{
				Collection<?> ntarget = (Collection<?>) target;
				if (!ntarget.isEmpty())
					wrTarget = ntarget.iterator().next();
				else
					wrTarget = null;
			}
		}
		handleAction(sender, wrTarget);
	}
	
	public boolean isNeeded()
	{
		if (callback != null)
		{
			return callback.showActionButton();
		}
		return true;
	}
	
	protected abstract void handleAction(Object sender, Object target);
	
	public interface ActionButtonCallback 
	{
		boolean showActionButton(); 
	}
}
