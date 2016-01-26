/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.utils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

import pl.edu.icm.unity.Constants;

/**
 * Time related utilities.
 * @author K. Benedyczak
 */
public class TimeUtil
{
	public static Date roundToS(Date what)
	{
		return what == null ? null : new Date((what.getTime()/1000)*1000);
	}
	
	/**
	 * @param instant
	 * @return instant formated as a string with medium size in the current timezone
	 */
	public static String formatMediumInstant(Instant instant)
	{
		return Constants.DT_FORMATTER_MEDIUM.format(
				LocalDateTime.ofInstant(instant, ZoneId.systemDefault()));
	}
}
