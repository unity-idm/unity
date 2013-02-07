/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.utils;

import eu.unicore.util.DefaultLogFactory;
import eu.unicore.util.Log;

/**
 * Generates logger categories for defaults from UNICORE libs
 * @author K. Benedyczak
 */
public class UnityLoggerFactory extends DefaultLogFactory
{
	@Override
	public String getLoggerName(String prefix, Class<?> clazz)
	{
		if (prefix.equals(Log.CONFIGURATION))
			return super.getLoggerName(pl.edu.icm.unity.server.utils.Log.U_SERVER_CFG, clazz);
		if (prefix.equals(Log.HTTP_SERVER))
			return super.getLoggerName(pl.edu.icm.unity.server.utils.Log.U_SERVER_CFG, clazz);
		return super.getLoggerName(prefix, clazz);
	}
}
