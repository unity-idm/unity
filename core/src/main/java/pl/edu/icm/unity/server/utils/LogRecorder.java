/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.utils;

import org.apache.log4j.Logger;

/**
 * 
 * @author Roman Krysinski
 *
 */
public class LogRecorder 
{
	protected StringRecorderAppender recorder;

	public void startLogRecording() 
	{
		recorder = new StringRecorderAppender();
		Logger.getRootLogger().addAppender(recorder);
	}

	public void stopLogRecording() 
	{
		Logger.getRootLogger().removeAppender(recorder);
	}
	
	public StringBuffer getCapturedLogs()
	{
		return recorder.getCapturedLogs();
	}
}
