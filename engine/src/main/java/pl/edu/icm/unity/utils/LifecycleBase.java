/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.utils;

import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

/**
 * Base for lifecycle handlers
 * @author K. Benedyczak
 */
@Component		
public abstract class LifecycleBase implements SmartLifecycle
{
	protected boolean running;
	
	@Override
	public void start()
	{
		running = true;
	}

	@Override
	public void stop()
	{
		running = false;
	}

	@Override
	public boolean isRunning()
	{
		return running;
	}

	@Override
	public boolean isAutoStartup()
	{
		return true;
	}

	@Override
	public void stop(Runnable callback) {stop(); callback.run();}
}