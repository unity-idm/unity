/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.events;

import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;

import pl.edu.icm.unity.db.DBEvents;
import pl.edu.icm.unity.db.model.ResolvedEventBean;
import pl.edu.icm.unity.server.events.EventListener;
import pl.edu.icm.unity.server.utils.Log;

/**
 * Processes heavy-weight events: takes pending ones from DB and tries to invoke them one by one.
 * 
 * @author K. Benedyczak
 */
public class EventsProcessingThread extends Thread
{
	private static final Logger log = Log.getLogger(Log.U_SERVER, EventsProcessingThread.class);
	public static final long INTERVAL = 30000;
	public static final long DELAY = 30000;
	public static final long MAX_DELAY = 3600000;
	private DBEvents dbEvents;
	private EventProcessor eventProcessor;
	
	public EventsProcessingThread(EventProcessor processor, DBEvents dbEvents)
	{
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
				List<ResolvedEventBean> events = dbEvents.getEventsForProcessing(new Date());
				for (ResolvedEventBean event:events)
					handleHeavyweightEvent(event);
			}
		}
	}
	
	public synchronized void wakeUp()
	{
		notify();
	}
	
	private void handleHeavyweightEvent(ResolvedEventBean event)
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
			dbEvents.removeEvent(event.getId());
			log.debug("Event " + event.getId() + " successfully handled");
		} else
		{
			int failures = event.getFailures() + 1;
			if (listener.getMaxFailures() <= failures)
			{
				log.warn("Dropping event for " + event.getListenerId() + " after too many failures");
				dbEvents.removeEvent(event.getId());
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
