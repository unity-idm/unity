/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common;

import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.basic.AttributeType;

/**
 * Shared helpers for attribute(types)
 * @author K. Benedyczak
 */
public class AttributeTypeUtils
{
	public static String getBoundsDesc(UnityMessageSource msg, int min, int max)
	{
		String from = min == Integer.MIN_VALUE ? msg.getMessage("AttributeType.noLimit") : min+"";
		String to = max == Integer.MAX_VALUE ? msg.getMessage("AttributeType.noLimit") : max+""; 
		return "[" + from + ", " + to + "]";
	}

	public static String getBoundsDesc(UnityMessageSource msg, long min, long max)
	{
		String from = min == Long.MIN_VALUE ? msg.getMessage("AttributeType.noLimit") : min+"";
		String to = max == Long.MAX_VALUE ? msg.getMessage("AttributeType.noLimit") : max+""; 
		return "[" + from + ", " + to + "]";
	}
	
	public static String getBoundsDesc(UnityMessageSource msg, double min, double max)
	{
		String from = min == Double.MIN_VALUE ? msg.getMessage("AttributeType.noLimit") : min+"";
		String to = max == Double.MAX_VALUE ? msg.getMessage("AttributeType.noLimit") : max+""; 
		return "[" + from + ", " + to + "]";
	}
	
	public static String getBooleanDesc(UnityMessageSource msg, boolean val)
	{
		return val ? msg.getMessage("yes") : msg.getMessage("no");
	}
	
	public static String getVisibilityDesc(UnityMessageSource msg, AttributeType type)
	{
		return msg.getMessage("AttributeType.visibility." + type.getVisibility().toString());
	} 

	public static String getFlagsDesc(UnityMessageSource msg, AttributeType type)
	{
		StringBuilder sb = new StringBuilder();
		int flags = type.getFlags();
		boolean needSep = false;
		if ((flags & AttributeType.INSTANCES_IMMUTABLE_FLAG) != 0)
		{
			sb.append(msg.getMessage("AttributeType.instancesImmutable"));
			needSep = true;
		}
		if ((flags & AttributeType.TYPE_IMMUTABLE_FLAG) != 0)
		{
			if (needSep)
				sb.append("; ");
			sb.append(msg.getMessage("AttributeType.typeImmutable"));
			needSep = true;
		}
		return sb.toString();
	} 
}
