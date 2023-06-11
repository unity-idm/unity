/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.events;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.event.Event;
import pl.edu.icm.unity.base.event.EventExecution;
import pl.edu.icm.unity.base.event.PersistableEvent;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.event.EventListener;
import pl.edu.icm.unity.engine.api.event.EventListenersManagement;
import pl.edu.icm.unity.engine.api.event.EventPublisher;
import pl.edu.icm.unity.engine.api.utils.ExecutorsService;
import pl.edu.icm.unity.store.api.EventDAO;
import pl.edu.icm.unity.store.api.tx.TransactionalRunner;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Takes events from producers and dispatches them to all registered {@link EventListener}s.
 * This class is thread safe.
 */
@Component
public class EventProcessor implements EventPublisher, EventListenersManagement
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_EVENT, EventProcessor.class);
	private Set<EventListener> listeners = new HashSet<>();
	private Map<String, EventListener> listenersById = new HashMap<>();
	private ReadWriteLock lock = new ReentrantReadWriteLock();
	
	private ExecutorService executorService;
	private EventDAO dbEvents;
	private EventsProcessingThread asyncProcessor;
	private TransactionalRunner tx;
	
	@Autowired
	public EventProcessor(ExecutorsService executorsService, EventDAO dbEvents,
			TransactionalRunner tx)
	{
		executorService = executorsService.getExecutionService();
		this.dbEvents = dbEvents;
		this.tx = tx;
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
					new VolatileListenerInvoker(listener, event) :
					new ReliableListenerInvoker(listener, event);
			if (listener.isAsync(event))
				executorService.submit(task);
			else
				executeNow(listener, event, task);
		}
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
		eventListener.init();
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
	
	@Override
	public Set<EventListener> getListeners()
	{
		return listeners;
	}
	
	public int getPendingEventsNumber()
	{
		return tx.runInTransactionRet(() -> dbEvents.getEligibleForProcessing(new Date(System.currentTimeMillis() +
				EventsProcessingThread.MAX_DELAY*100000)).size());
	}
	
	EventListener getListenerById(String id)
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
	 * Invokes the volatile listener
	 * @author K. Benedyczak
	 */
	private static class VolatileListenerInvoker implements Callable<Void>
	{
		private EventListener listener;
		private Event event;

		VolatileListenerInvoker(EventListener listener, Event event)
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
					log.warn("Volatile event listener " + listener.getId() +
						" failed when processing an event " + event);
			} catch (Exception t)
			{
				log.warn("Volatile event listener " + listener.getId() +
						" crashed when processing an event " + event, t);
			}
			return null;
		}
	}

	/**
	 * Only adds the reliable listener's event to the DB queue.
	 * @author K. Benedyczak
	 */
	private class ReliableListenerInvoker implements Callable<Void>
	{
		private PersistableEvent event;
		private String listenerId;

		ReliableListenerInvoker(EventListener listener, Event event)
		{
			if (!(event instanceof PersistableEvent)) {
				throw new IllegalArgumentException("Event has to be PersistableEvent instance to be handled by ReliableListenerInvoker. Check Listener isWanted() implementation.");
			}
			this.event = (PersistableEvent)event;
			listenerId = listener.getId();
		}

		@Override
		public Void call() throws Exception
		{
			EventExecution newEvent = new EventExecution(event, new Date(0), listenerId, 0);
			tx.runInTransaction(()->dbEvents.create(newEvent));
			asyncProcessor.wakeUp();
			return null;
		}
	}
}
