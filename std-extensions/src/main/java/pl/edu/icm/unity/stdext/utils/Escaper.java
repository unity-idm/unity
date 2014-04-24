/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.stdext.utils;

import java.util.ArrayList;
import java.util.List;


/**
 * Allows to encode or decode a string from several arguments. The arguments are concatenated with a given
 * character and are properly escaped.
 * @author K. Benedyczak
 */
public class Escaper
{
	public static String encode(String... args)
	{
		if (args == null || args.length == 0)
			return "";
		StringBuilder ret = new StringBuilder(args[0].length()*args.length*2);
		
		for (int i=0; i<args.length; i++)
		{
			appendEscaped(ret, args[i]);
			if (i<args.length-1)
				ret.append('$');
		}
		return ret.toString();
	}

	public static String[] decode(String encoded)
	{
		List<String> ret = new ArrayList<>();
		char[] chars = encoded.toCharArray();
		boolean escaped = false;
		StringBuilder current = new StringBuilder();
		for (int i=0; i<chars.length; i++)
		{
			switch (chars[i])
			{
			case '\\':
				if (escaped)
				{
					escaped = false;
					current.append('\\');
				} else
				{
					escaped = true;
				}
				break;
			case '$':
				if (escaped)
				{
					escaped = false;
					current.append('$');
				} else
				{
					ret.add(current.toString());
					current = new StringBuilder();
				}
				break;
			default:
				current.append(chars[i]);
			}
		}
		if (!current.equals(""))
			ret.add(current.toString());
		return ret.toArray(new String[ret.size()]);
	}
	
	
	private static void appendEscaped(StringBuilder sb, String arg)
	{
		char[] chars = arg.toCharArray();
		for (int i=0; i<chars.length; i++)
		{
			if (chars[i] == '\\' || chars[i] == '$')
				sb.append('\\');
			sb.append(chars[i]);
		}
	}
}
