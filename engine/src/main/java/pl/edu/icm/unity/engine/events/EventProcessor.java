/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.events;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.db.DBEvents;
import pl.edu.icm.unity.server.events.Event;
import pl.edu.icm.unity.server.events.EventListener;
import pl.edu.icm.unity.server.utils.ExecutorsService;
import pl.edu.icm.unity.server.utils.Log;

/**
 * Takes events from producers and dispatches them to all registered {@link EventListener}s.
 * This class is thread safe.
 * @author K. Benedyczak
 */
@Component
public class EventProcessor
{
	private static final Logger log = Log.getLogger(Log.U_SERVER, EventProcessor.class);
	private Map<String, Set<EventListener>> listenersByCategory = new HashMap<String, Set<EventListener>>();
	private Map<String, EventListener> listenersById = new HashMap<String, EventListener>();
	private ReadWriteLock lock = new ReentrantReadWriteLock();
	
	private ScheduledExecutorService executorService;
	private DBEvents dbEvents;
	private EventsProcessingThread asyncProcessor;
	
	@Autowired
	public EventProcessor(ExecutorsService executorsService, DBEvents dbEvents)
	{
		executorService = executorsService.getService();
		this.dbEvents = dbEvents;
		this.asyncProcessor = new EventsProcessingThread(this, dbEvents);
		this.asyncProcessor.start();
	}


	public void fireEvent(Event event)
	{
		List<EventListener> interestedListeners = getInterestedListeners(event);
		if (interestedListeners == null)
			return;
		
		for (EventListener listener: interestedListeners)
		{
			Callable<Void> task = listener.isLightweight() ? 
					new LightweightListenerInvoker(listener, event) :
					new HeavyweightListenerInvoker(listener, event);
			executorService.submit(task);
		}
	}
	
	public void addEventListener(EventListener eventListener)
	{
		lock.writeLock().lock();
		try
		{
			Set<EventListener> listeners = listenersByCategory.get(eventListener.getCategory());
			if (listeners == null)
			{
				listeners = new HashSet<EventListener>();
				listenersByCategory.put(eventListener.getCategory(), listeners);
			}
			listeners.add(eventListener);
			listenersById.put(eventListener.getId(), eventListener);
		} finally
		{
			lock.writeLock().unlock();
		}
	}
	
	public synchronized void removeEventListener(EventListener eventListener)
	{
		lock.writeLock().lock();
		try
		{
			Set<EventListener> listeners = listenersByCategory.get(eventListener.getCategory());
			if (listeners != null)
				listeners.remove(eventListener);
			listenersById.remove(eventListener.getId());
		} finally
		{
			lock.writeLock().unlock();
		}
	}
	
	public int getPendingEventsNumber()
	{
		return dbEvents.getEventsForProcessing(new Date(System.currentTimeMillis() + 
				EventsProcessingThread.MAX_DELAY*100000)).size();
	}
	
	public EventListener getListenerById(String id)
	{
		lock.readLock().lock();
		try
		{
			return listenersById.get(id);
		} finally
		{
			lock.readLock().unlock();
		}
	}
	
	private List<EventListener> getInterestedListeners(Event event)
	{
		List<EventListener> interestedListeners;
		lock.readLock().lock();
		try
		{
			Set<EventListener> listeners = listenersByCategory.get(event.getCategory());
			if (listeners == null)
				return null;
			interestedListeners = new ArrayList<EventListener>(listeners.size());
			for (EventListener listener: listeners)
			{
				if (listener.isWanted(event))
					interestedListeners.add(listener);
			}
			return interestedListeners;
		} finally
		{
			lock.readLock().unlock();
		}
	}
	
	/**
	 * Invokes the lightweight listener
	 * @author K. Benedyczak
	 */
	private static class LightweightListenerInvoker implements Callable<Void>
	{
		private EventListener listener;
		private Event event;

		public LightweightListenerInvoker(EventListener listener, Event event)
		{
			this.listener = listener;
			this.event = event;
		}

		@Override
		public Void call() throws Exception
		{
			try
			{
				if (!listener.handleEvent(event))
					log.warn("Ligthweight event listener " + listener.getId() + 
						" failed when processing an event " + event);
			} catch (Exception t)
			{
				log.warn("Ligthweight event listener " + listener.getId() + 
						" creshed when processing an event " + event, t);
			}
			return null;
		}
	}

	/**
	 * Only adds the heavyweight listener's event to the DB queue.
	 * @author K. Benedyczak
	 */
	private class HeavyweightListenerInvoker implements Callable<Void>
	{
		private Event event;
		private String listenerId;

		public HeavyweightListenerInvoker(EventListener listener, Event event)
		{
			this.event = event;
			listenerId = listener.getId();
		}

		@Override
		public Void call() throws Exception
		{
			dbEvents.addEvent(event, listenerId);
			asyncProcessor.wakeUp();
			return null;
		}
	}
}
