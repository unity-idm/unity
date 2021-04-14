/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.events;

import java.util.Date;
import java.util.List;

import org.apache.logging.log4j.Logger;

import pl.edu.icm.unity.base.event.EventExecution;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.event.EventListener;
import pl.edu.icm.unity.store.api.EventDAO;
import pl.edu.icm.unity.store.api.tx.TransactionalRunner;

/**
 * Processes heavy-weight events: takes pending ones from DB and tries to invoke them one by one.
 * 
 * @author K. Benedyczak
 */
public class EventsProcessingThread extends Thread
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_EVENT, EventsProcessingThread.class);
	public static final long INTERVAL = 30000;
	public static final long DELAY = 30000;
	public static final long MAX_DELAY = 3600000;
	private EventDAO dbEvents;
	private EventProcessor eventProcessor;
	private TransactionalRunner tx;
	
	public EventsProcessingThread(EventProcessor processor, EventDAO dbEvents, TransactionalRunner tx)
	{
		this.tx = tx;
		setDaemon(true);
		this.dbEvents = dbEvents;
		this.eventProcessor = processor;
	}
	
	public void run()
	{
		while(true)
		{
			synchronized(this)
			{
				try
				{
					wait(INTERVAL);
				} catch (InterruptedException e) {}

				tx.runInTransaction(() -> {
					List<EventExecution> events = dbEvents.getEligibleForProcessing(new Date());
					for (EventExecution event: events)
						handleHeavyweightEvent(event);
				});
			}
		}
	}
	
	public synchronized void wakeUp()
	{
		notify();
	}
	
	private void handleHeavyweightEvent(EventExecution event)
	{
		EventListener listener = eventProcessor.getListenerById(event.getListenerId());
		if (listener == null)
		{
			log.info("Dropping event for not existing listener " + event.getListenerId());
			return;
		}
		
		boolean result;
		try
		{
			if (log.isDebugEnabled())
				log.debug("Handling heavyweight event " + event);
			result = listener.handleEvent(event.getEvent());
			if (!result)
				log.info("Event processing failed by " + listener.getId());
		} catch (Exception t)
		{
			log.warn("Event for " + event.getListenerId() + " thrown an exception", t);
			result = false;
		}

		if (result)
		{
			dbEvents.deleteByKey(event.getId());
			log.debug("Event " + event.getId() + " successfully handled");
		} else
		{
			int failures = event.getFailures() + 1;
			if (listener.getMaxFailures() <= failures)
			{
				log.warn("Dropping event for " + event.getListenerId() + " after too many failures");
				dbEvents.deleteByKey(event.getId());
				return;
			}
			Date newExecution = new Date(System.currentTimeMillis() + getDelay(failures));
			dbEvents.updateExecution(event.getId(), newExecution, failures);
		}
	}
	
	private long getDelay(int failures)
	{
		return failures < 10 ? failures * DELAY : MAX_DELAY; 
	}
}
