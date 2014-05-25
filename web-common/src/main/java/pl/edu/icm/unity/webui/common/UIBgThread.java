/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common;

import org.apache.log4j.Logger;

import pl.edu.icm.unity.server.authn.InvocationContext;
import pl.edu.icm.unity.server.utils.Log;

import com.vaadin.server.VaadinSession;
import com.vaadin.ui.UI;
import com.vaadin.util.CurrentInstance;

/**
 * Wrapper of Runnables that are run in background thread and are going to access 
 * either Vaadin's thread-local state (sessions etc) or Unity threadLocal state (e.g. current locale, 
 * usage of message bundle).
 *  <p>
 *  Constructor must be called in a properly initialized thread, typically in the main UI-action handling thread.
 * @author K. Benedyczak
 */
public abstract class UIBgThread implements Runnable
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, UIBgThread.class);
	private VaadinSession session;
	private InvocationContext unityContext;
	private UI ui;

	
	public UIBgThread()
	{
		unityContext = InvocationContext.getCurrent();
		session = VaadinSession.getCurrent();
		ui = UI.getCurrent();
		
		if (ui == null || session == null || unityContext == null)
		{
			//we need a stack trace
			try
			{
				throw new IllegalStateException();
			} catch (Exception e)
			{
				log.error("UI BG thread created with UI=" + ui +
						" session=" + session +
						" context=" + unityContext, e);
			}
		}
	}
	
	@Override
	public final void run()
	{
		try
		{
			VaadinSession.setCurrent(session);
			InvocationContext.setCurrent(unityContext);
			UI.setCurrent(ui);
			
			safeRun();
		} catch (Exception e)
		{
			log.error("Background action failed", e);
		} finally
		{
			InvocationContext.setCurrent(null);
			CurrentInstance.clearAll();
		}
	}
	
	public abstract void safeRun();
}
