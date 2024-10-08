/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.endpoint.common;

import com.vaadin.flow.server.VaadinSession;
import io.imunity.vaadin.endpoint.common.bus.EventsBus;

public class WebSession
{
	private final EventsBus eventBus;
	
	public static WebSession getCurrent()
	{
		VaadinSession vaadinSession = VaadinSession.getCurrent();
		if (vaadinSession == null)
			return null;
		WebSession attribute = vaadinSession.getAttribute(WebSession.class);
		if(attribute == null) {
			WebSession webSession = new WebSession(new EventsBus());
			vaadinSession.setAttribute(WebSession.class, webSession);
			return webSession;
		}
		return attribute;
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
