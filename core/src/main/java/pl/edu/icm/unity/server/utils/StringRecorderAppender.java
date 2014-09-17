/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.utils;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.spi.LoggingEvent;

/**
 * 
 * @author Roman Krysinski
 *
 */
public class StringRecorderAppender extends AppenderSkeleton 
{
	private static final String PATTERN = "%d %x %m%n";
	private StringBuffer recording;
	private String threadName;  

	public StringRecorderAppender(String threadName)
	{
		this.threadName = threadName;
		recording = new StringBuffer();
		layout = new PatternLayout(PATTERN);
	}
	
	@Override
	protected void append(LoggingEvent event) 
	{
		if (threadName.equals(event.getThreadName()))
		{
			recording.append(this.layout.format(event));
		}
	}

	public StringBuffer getCapturedLogs()
	{
		return recording;
	}
	
	@Override
	public void close() 
	{
		// nop
	}
	
	@Override
	public boolean requiresLayout() 
	{
		return true;
	}
}
