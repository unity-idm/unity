/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.utils;

import java.io.StringWriter;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.WriterAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.layout.PatternLayout;

import com.google.common.collect.Sets;

/**
 * 
 * @author Roman Krysinski
 *
 */
public class LogRecorder 
{
	private static final String PATTERN = "%d %x %m%n";
	private StringWriter writer;
//	private Map<String, Level> levelBackup;
	private static AtomicInteger instances = new AtomicInteger(0);
	private Set<String> facilities;
	private String appenderName;
	
	public LogRecorder(String... facilities)
	{
		this.facilities = Sets.newHashSet(facilities);
	}

	public void startLogRecording() 
	{
		instances.incrementAndGet();
		writer = new StringWriter(100000);
		addAppender();
		
//		levelBackup = new HashMap<String, Level>();
//		for (String log : facilities) 
//		{
//			Logger logger = Logger.getRootLogger().getLoggerRepository().getLogger(log);
//			levelBackup.put(log, logger.getLevel());
//			logger.setLevel(Level.ALL);
//			logger.addAppender(recorder);
//		}
	}

	public void stopLogRecording() 
	{
		if (instances.decrementAndGet() == 0)
		{
			removeAppender();
//			for (String log : facilities) 
//			{
//				Logger logger = Logger.getRootLogger().getLoggerRepository().getLogger(log);
//				logger.removeAppender(recorder);
//				logger.setLevel(levelBackup.get(log));
//			}
		}
	}
	


	private void addAppender() 
	{
		LoggerContext context = LoggerContext.getContext(false);
		Configuration config = context.getConfiguration();
		PatternLayout layout = PatternLayout.newBuilder().withPattern(PATTERN).build();
		appenderName = Thread.currentThread().getName() + "-" + UUID.randomUUID();
		Appender appender = WriterAppender.createAppender(layout, null, writer, appenderName, false, true);
		appender.start();
		//needed? if yes then we are in trouble - memory leak possible as there is no remove...
		//config.addAppender(appender);
		addAppenderToLoggers(appender, config);
	}

	private void removeAppender() 
	{
		LoggerContext context = LoggerContext.getContext(false);
		Configuration config = context.getConfiguration();
		//needed? if yes then we are in trouble - memory leak possible as there is no remove...
		//config.addAppender(appender);
		removeAppenderFromLoggers(config);
	}

	private void addAppenderToLoggers(final Appender appender, final Configuration config) 
	{
		for (LoggerConfig loggerConfig : config.getLoggers().values()) 
		{
			if (facilities.contains(loggerConfig.getName()))
				loggerConfig.addAppender(appender, Level.ALL, null);
		}
	}

	private void removeAppenderFromLoggers(final Configuration config) 
	{
		for (LoggerConfig loggerConfig : config.getLoggers().values()) 
		{
			if (facilities.contains(loggerConfig.getName()))
				loggerConfig.removeAppender(appenderName);
		}
	}

	public StringBuffer getCapturedLogs()
	{
		return writer.getBuffer();
	}
}
