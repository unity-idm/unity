/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.utils;

import org.apache.log4j.Logger;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.utils.Log;

/**
 * Base code for scheduled updaters. Extensions should implement the actual update logic,
 * the code in this base class ensures proper timing of executions, properly synchronized not to loose updates.
 * @author K. Benedyczak
 */
public abstract class ScheduledUpdaterBase implements Runnable
{
	private static final Logger log = Log.getLogger(Log.U_SERVER, ScheduledUpdaterBase.class);
	private String info;
	private long lastUpdate = 0;
	
	public ScheduledUpdaterBase(String info)
	{
		this.info = info;
	}

	/**
	 * Invokes a refresh, ensuring that updates which took place during the same second as the time of the call are
	 * included. The method invocation can wait up to 1s. 
	 * @throws EngineException
	 */
	public void updateManual() throws EngineException
	{
		long start = roundToS(System.currentTimeMillis());
		while (roundToS(System.currentTimeMillis()) == start)
		{
			try
			{
				Thread.sleep(100);
			} catch (InterruptedException e)
			{
				//ok
			}
		}
		update();
	}
	
	public void update() throws EngineException
	{
		synchronized(this)
		{
			updateInternal();
		}
	}

	protected long roundToS(long ts)
	{
		return (ts/1000)*1000;
	}
	
	/**
	 * Sets the initial update time, ensuring that there won't be an immediate re-update.
	 * @param updateTime
	 */
	public void setInitialUpdate(long updateTime)
	{
		setLastUpdate(updateTime + 1000);
	}
	
	protected void setLastUpdate(long lastUpdate)
	{
		synchronized(this)
		{
			this.lastUpdate = roundToS(lastUpdate);
		}
	}
	
	protected long getLastUpdate()
	{
		return this.lastUpdate;
	}
	
	public void run()
	{
		try
		{
			update();
		} catch (Exception e)
		{
			log.error("Can't synchronize runtime state of " + info +
					"with the persisted state", e);
		}
	}
	
	protected abstract void updateInternal() throws EngineException;
}
