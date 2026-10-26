/*
 * Copyright (c) 2026 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.attribute.cache;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;

/**
 * Captures formatted messages of a single logger at the given level, for the time until closed.
 */
class LogCapture extends AbstractAppender implements AutoCloseable
{
	private final Logger logger;
	private final Level previousLevel;
	private final List<String> messages = Collections.synchronizedList(new ArrayList<>());

	private LogCapture(Logger logger, Level level)
	{
		super("test-capture", null, null, true, Property.EMPTY_ARRAY);
		this.logger = logger;
		this.previousLevel = logger.getLevel();
		start();
		logger.addAppender(this);
		logger.setLevel(level);
	}

	static LogCapture capture(org.apache.logging.log4j.Logger logger, Level level)
	{
		return new LogCapture((Logger) LogManager.getLogger(logger.getName()), level);
	}

	@Override
	public void append(LogEvent event)
	{
		messages.add(event.getMessage().getFormattedMessage());
	}

	List<String> getMessages()
	{
		return List.copyOf(messages);
	}

	@Override
	public void close()
	{
		logger.removeAppender(this);
		logger.setLevel(previousLevel);
		stop();
	}
}
