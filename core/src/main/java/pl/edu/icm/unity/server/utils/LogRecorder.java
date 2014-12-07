/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

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
	private Map<String, Level> levelBackup;
	private static AtomicInteger instances = new AtomicInteger(0);
	private static final String DEFAULT_LOGS_TO_RECORD[] = {
		Log.U_SERVER_TRANSLATION, 
		Log.U_SERVER_SAML,
		Log.U_SERVER_OAUTH,
		Log.U_SERVER_LDAP,
		Log.U_SERVER_WEB
	};
	

	public void startLogRecording() 
	{
		instances.incrementAndGet();
		recorder    = new StringRecorderAppender((Thread.currentThread()).getName());
		levelBackup = new HashMap<String, Level>();
		
		for (String log : DEFAULT_LOGS_TO_RECORD) 
		{
			Logger logger = Logger.getRootLogger().getLoggerRepository().getLogger(log);
			levelBackup.put(log, logger.getLevel());
			logger.setLevel(Level.ALL);
			logger.addAppender(recorder);
		}
	}

	public void stopLogRecording() 
	{
		if (instances.decrementAndGet() == 0)
		{
			for (String log : DEFAULT_LOGS_TO_RECORD) 
			{
				Logger logger = Logger.getRootLogger().getLoggerRepository().getLogger(log);
				logger.removeAppender(recorder);
				logger.setLevel(levelBackup.get(log));
			}
		}
	}
	
	public StringBuffer getCapturedLogs()
	{
		return recorder.getCapturedLogs();
	}
}
