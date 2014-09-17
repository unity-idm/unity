/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.utils;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
 * 
 * @author Roman Krysinski
 *
 */
public class LogRecorder 
{
	protected StringRecorderAppender recorder;
	protected Logger targetLogger;
	private Level backup; 

	public void startLogRecording() 
	{
		recorder = new StringRecorderAppender((Thread.currentThread()).getName());
		targetLogger = Logger.getRootLogger().getLoggerRepository().getLogger(Log.U_SERVER_TRANSLATION);
		backup = targetLogger.getLevel();
		targetLogger.setLevel(Level.ALL);
		targetLogger.addAppender(recorder);
	}

	public void stopLogRecording() 
	{
		targetLogger.removeAppender(recorder);
		targetLogger.setLevel(backup);
	}
	
	public StringBuffer getCapturedLogs()
	{
		return recorder.getCapturedLogs();
	}
}
