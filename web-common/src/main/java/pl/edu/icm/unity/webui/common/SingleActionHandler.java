/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common;

import java.util.Collection;

import com.google.gwt.thirdparty.guava.common.collect.SetMultimap;
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
				if (t.isEmpty() && needsTarget)
					return EMPTY;
				if (t.size() > 1 && !multiTarget && needsTarget)
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
		handleAction(sender, target);
	}
	
	protected abstract void handleAction(Object sender, Object target);
}
