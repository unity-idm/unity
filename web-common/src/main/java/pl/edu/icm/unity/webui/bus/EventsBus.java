/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.bus;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import pl.edu.icm.unity.server.utils.Log;

/**
 * Manages events and their listeners: producers can send events to this object, listeners register
 * and the bus will broadcast each event to all listeners registered for it.
 *  
 * @author K. Benedyczak
 */
public class EventsBus 
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, EventsBus.class);
	private Map<Class<?>, List<EventListener<?>>> listeners = new HashMap<Class<?>, List<EventListener<?>>>();
	
	
	public synchronized <T extends Event> void addListener(EventListener<T> listener, Class<T> eventType)
	{
		List<EventListener<?>> typeListeners = listeners.get(eventType);
		if (typeListeners == null)
		{
			typeListeners = new ArrayList<EventListener<?>>(8);
			listeners.put(eventType, typeListeners);
		}
		typeListeners.add(listener);
	}
	
	public synchronized <T extends Event> void removeListener(EventListener<T> listener, Class<T> eventType)
	{
		List<EventListener<?>> typeListeners = listeners.get(eventType);
		if (typeListeners != null)
			typeListeners.remove(listener);
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public synchronized void fireEvent(Event event)
	{
		List<EventListener<?>> typeListeners = listeners.get(event.getClass());
		for (EventListener listener: typeListeners)
		{
			try
			{
				listener.handleEvent(event);
			} catch (Exception e)
			{
				log.error("Invoking event handler for event " + event.getClass() + " failed", e);
			}
		}
	}
}
