/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common;

import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.types.basic.AttributeType;

/**
 * Shared helpers for attribute(types)
 * @author K. Benedyczak
 */
public class AttributeTypeUtils
{
	public static String getBoundsDesc(MessageSource msg, Integer min, Integer max)
	{
		String from = (min == null || min == Integer.MIN_VALUE) ? msg.getMessage("AttributeType.noLimit") : min+"";
		String to = (max == null || max == Integer.MAX_VALUE) ? msg.getMessage("AttributeType.noLimit") : max+""; 
		return "[" + from + ", " + to + "]";
	}

	public static String getBoundsDesc(MessageSource msg, Long min, Long max)
	{
		String from = (min == null || min == Long.MIN_VALUE) ? msg.getMessage("AttributeType.noLimit") : min+"";
		String to = (max == null || max == Long.MAX_VALUE) ? msg.getMessage("AttributeType.noLimit") : max+""; 
		return "[" + from + ", " + to + "]";
	}
	
	public static String getBoundsDesc(MessageSource msg, Double min, Double max)
	{
		String from = (min == null || min == Double.MIN_VALUE) ? msg.getMessage("AttributeType.noLimit") : min+"";
		String to = (max == null || max == Double.MAX_VALUE) ? msg.getMessage("AttributeType.noLimit") : max+""; 
		return "[" + from + ", " + to + "]";
	}
	
	public static String getBooleanDesc(MessageSource msg, boolean val)
	{
		return val ? msg.getMessage("yes") : msg.getMessage("no");
	}
	
	public static String getFlagsDesc(MessageSource msg, AttributeType type)
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
