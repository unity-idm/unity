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

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.event.Event;
import pl.edu.icm.unity.base.event.EventExecution;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.event.EventListener;
import pl.edu.icm.unity.engine.api.event.EventPublisher;
import pl.edu.icm.unity.engine.api.utils.ExecutorsService;
import pl.edu.icm.unity.engine.authz.InternalAuthorizationManager;
import pl.edu.icm.unity.engine.authz.AuthzCapability;
import pl.edu.icm.unity.exceptions.AuthorizationException;
import pl.edu.icm.unity.store.api.EventDAO;
import pl.edu.icm.unity.store.api.tx.TransactionalRunner;

/**
 * Takes events from producers and dispatches them to all registered {@link EventListener}s.
 * This class is thread safe.
 * @author K. Benedyczak
 */
@Component
public class EventProcessor implements EventPublisher
{
	private static final Logger log = Log.getLogger(Log.U_SERVER, EventProcessor.class);
	private Set<EventListener> listeners = new HashSet<EventListener>();
	private Map<String, EventListener> listenersById = new HashMap<String, EventListener>();
	private ReadWriteLock lock = new ReentrantReadWriteLock();
	
	private ScheduledExecutorService executorService;
	private EventDAO dbEvents;
	private EventsProcessingThread asyncProcessor;
	private InternalAuthorizationManager authz;
	
	@Autowired
	public EventProcessor(ExecutorsService executorsService, EventDAO dbEvents,
			InternalAuthorizationManager authz,
			TransactionalRunner tx)
	{
		this.authz = authz;
		executorService = executorsService.getService();
		this.dbEvents = dbEvents;
		this.asyncProcessor = new EventsProcessingThread(this, dbEvents, tx);
		this.asyncProcessor.start();
	}

	@Override
	public void fireEvent(Event event)
	{
		List<EventListener> interestedListeners = getInterestedListeners(event);
		if (interestedListeners == null)
			return;
		
		if (log.isDebugEnabled())
			log.debug("Fire event: {}", event);
		for (EventListener listener: interestedListeners)
		{
			Callable<Void> task = listener.isLightweight() ? 
					new LightweightListenerInvoker(listener, event) :
					new HeavyweightListenerInvoker(listener, event);
			if (listener.isAsync(event))
				executorService.submit(task);
			else
				executeNow(listener, event, task);
		}
	}

	@Override
	public void fireEventWithAuthz(Event event) throws AuthorizationException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		fireEvent(event);
	}
	
	private void executeNow(EventListener listener, Event event, Callable<Void> task)
	{
		log.trace("Handling event in sync mode {}", event);
		try
		{
			task.call();
		} catch (Exception e)
		{
			log.error("Error invoking sync event processor for " + 
					listener.getId() + " event was " + event, e);
		}
	}

	public void addEventListener(EventListener eventListener)
	{
		lock.writeLock().lock();
		try
		{
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
			listeners.remove(eventListener);
			listenersById.remove(eventListener.getId());
		} finally
		{
			lock.writeLock().unlock();
		}
	}
	
	public int getPendingEventsNumber()
	{
		return dbEvents.getEligibleForProcessing(new Date(System.currentTimeMillis() + 
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
						" crashed when processing an event " + event, t);
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
			EventExecution newEvent = new EventExecution(event, new Date(0), listenerId, 0);
			dbEvents.create(newEvent);
			asyncProcessor.wakeUp();
			return null;
		}
	}
}
