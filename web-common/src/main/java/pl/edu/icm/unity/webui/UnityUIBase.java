/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licensing information.
 */
package pl.edu.icm.unity.webui;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.LinkedList;
import java.util.Queue;

import org.apache.log4j.Logger;

import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.webui.authn.CancelHandler;
import pl.edu.icm.unity.webui.common.ErrorPopup;

import com.vaadin.server.DefaultErrorHandler;
import com.vaadin.server.RequestHandler;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinResponse;
import com.vaadin.server.VaadinService;
import com.vaadin.server.VaadinSession;
import com.vaadin.server.WrappedSession;
import com.vaadin.ui.UI;

/**
 * All Unity {@link UI}s should extend this class. It provides a common logic. 
 * Currently proper error handling of unchecked exceptions.
 * @author K. Benedyczak
 */
public abstract class UnityUIBase extends UI implements UnityWebUI
{
	/**
	 * Under this key a {@link Queue} of Runnables is stored. The runnables are run by the 
	 * {@link RequestsContextQueueHandler}.
	 */
	public static final String ACTIONS_LIST_KEY = "pl.edu.icm.unity.web.WebSession.actionsList";
	
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, UnityUIBase.class);
	
	protected UnityMessageSource msg;
	protected CancelHandler cancelHandler;
	
	public UnityUIBase(UnityMessageSource msg)
	{
		super();
		this.msg = msg;
	}

	@Override
	protected final void init(VaadinRequest request)
	{
		setErrorHandler(new ErrorHandlerImpl());
		RequestsContextQueueHandler handler = new RequestsContextQueueHandler();
		VaadinSession.getCurrent().addRequestHandler(handler);
		handler.executePendingActions();
		appInit(request);
	}


	@Override
	public void setCancelHandler(CancelHandler handler)
	{
		this.cancelHandler = handler;
	}
	
	/**
	 * Same as Vaadin's {@link #init(VaadinRequest)}, separated so 
	 * it is not possible to forgot to call super.init().
	 * @param request
	 */
	protected abstract void appInit(VaadinRequest request);
	
	private class ErrorHandlerImpl extends DefaultErrorHandler 
	{
		@Override
		public void error(com.vaadin.server.ErrorEvent event) {
			
			log.error("UI code got an unchecked and not handled properly exception: " 
					+ event.getThrowable(), event.getThrowable());
			
			ErrorPopup.showError(msg, msg.getMessage("error"), 
					msg.getMessage("UnityUIBase.unhandledError"));
		} 
	}
	
	/**
	 * This class takes all actions queued in a {@link WebSession} and invokes them in order.
	 * The purpose of this mechanism is to perform operations which require HTTP request or response
	 * as cookie setting, which are initiated in background threads where the request&response are not available. 
	 * @author K. Benedyczak
	 */
	private class RequestsContextQueueHandler implements RequestHandler
	{
		@Override
		public boolean handleRequest(VaadinSession session, VaadinRequest request,
				VaadinResponse response) throws IOException
		{
			executePendingActions();
			return false;
		}
		
		public void executePendingActions()
		{
			Queue<Runnable> actions = getHttpContextActions();
			for (Runnable action: actions)
			{
				log.debug("Found pending action to be executed in HTTP context: " + action);
				try
				{
					action.run();
				} catch (Exception e)
				{
					log.error("Can not execute pending action in HTTP context action failed", e);
				}
			}
		}
	}
	
	/**
	 * Adds an action to be performed either immediately (if executing in the request handling thread) or
	 * later on when being in the client request handling thread. It is guaranteed that {@link VaadinRequest} and 
	 * {@link VaadinResponse} are available in the context. Useful for setting cookies.
	 * @param action
	 */
	public static void addHttpContextAction(Runnable action)
	{
		VaadinSession vs = VaadinSession.getCurrent();
		
		if (VaadinService.getCurrentResponse() != null && VaadinService.getCurrentRequest() != null)
		{
			log.debug("HTTP context action will be invoked immediately");
			vs.lock();
			try
			{
				action.run();
			} finally 
			{
				vs.unlock();
			}
			return;
		}
			
		WrappedSession ws = vs.getSession();
		log.debug("HTTP context action will be queued");
		synchronized (ws)
		{
			@SuppressWarnings("unchecked")
			Queue<Runnable> actions = (Queue<Runnable>) ws.getAttribute(ACTIONS_LIST_KEY);
			if (actions == null)
			{
				actions = new LinkedList<Runnable>();
				ws.setAttribute(ACTIONS_LIST_KEY, actions);
			}
			actions.add(action);
		}
	}
	
	private static Queue<Runnable> getHttpContextActions()
	{
		WrappedSession ws = VaadinSession.getCurrent().getSession();
		synchronized(ws)
		{
			@SuppressWarnings("unchecked")
			Queue<Runnable> actions = (Queue<Runnable>) ws.getAttribute(ACTIONS_LIST_KEY);
			Queue<Runnable> ret = actions == null ? new ArrayDeque<Runnable>() 
					: new ArrayDeque<Runnable>(actions);
			if (actions != null)
				actions.clear();
			return ret; 
		}
	}
}
