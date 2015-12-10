/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.utils;

import java.text.MessageFormat;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Utility class useful for micro benchmarking. 
 * When object is created it records current time. It can be then called to report 
 * cumulative time and the time from the last check.
 * @author K. Benedyczak
 */
public class StopWatch
{
	private Instant start;
	private Instant lastCheck;
	
	public StopWatch()
	{
		start = Instant.now();
		lastCheck = start;
	}
	
	/**
	 * time will be put in {0} place of the parameter.
	 * @param stringFormat
	 */
	public void printTotal(String stringFormat)
	{
		lastCheck = Instant.now();
		System.out.println(MessageFormat.format(stringFormat, start.until(lastCheck, ChronoUnit.MILLIS)));
	}
	
	/**
	 * time will be put in {0} place of the parameter.
	 * @param stringFormat
	 */
	public void printPeriod(String stringFormat)
	{
		Instant now = Instant.now();
		System.out.println(MessageFormat.format(stringFormat, lastCheck.until(now, ChronoUnit.MILLIS)));
		lastCheck = now;
	}
	
}
