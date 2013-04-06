/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common;

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
	private Action[] action;

	public SingleActionHandler(String caption, Resource icon)
	{
		Action a = new Action(caption, icon);
		action = new Action[] {a};
	}

	@Override
	public Action[] getActions(Object target, Object sender)
	{
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
