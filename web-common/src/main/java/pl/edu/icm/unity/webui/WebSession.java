/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui;

import com.vaadin.server.VaadinSession;

import pl.edu.icm.unity.server.authn.AuthenticatedEntity;
import pl.edu.icm.unity.webui.bus.EventsBus;

/**
 * Holds information stored in the HTTP session.
 * @author K. Benedyczak
 */
public class WebSession
{
	/**
	 * Under this key, the object {@link AuthenticatedEntity} 
	 * with authenticated user is stored in the session.
	 */
	public static final String USER_SESSION_KEY = WebSession.class.getName();
	
	private EventsBus eventBus;
	
	public static WebSession getCurrent()
	{
		VaadinSession vaadinSession = VaadinSession.getCurrent();
		if (vaadinSession == null)
			return null;
		return vaadinSession.getAttribute(WebSession.class);
	}

	public static void setCurrent(WebSession instance)
	{
		VaadinSession vaadinSession = VaadinSession.getCurrent();
		if (vaadinSession == null)
			throw new IllegalStateException("No vaadin session");
		vaadinSession.setAttribute(WebSession.class, instance);
	}
	
	public WebSession(EventsBus eventBus)
	{
		this.eventBus = eventBus;
	}

	public EventsBus getEventBus()
	{
		return eventBus;
	}
}
