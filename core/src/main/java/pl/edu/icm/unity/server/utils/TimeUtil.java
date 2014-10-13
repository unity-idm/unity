/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.utils;

import java.util.Date;

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
}
