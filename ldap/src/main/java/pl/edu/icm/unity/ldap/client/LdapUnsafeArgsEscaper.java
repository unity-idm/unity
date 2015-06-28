/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.ldap.client;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Sets;

/**
 * Escapes potentially unsafe contents before using it in queries or DNs.
 * 
 * @author K. Benedyczak
 */
public class LdapUnsafeArgsEscaper
{
	private static Set<Character> UNSAFE_FOR_DN = Sets.newHashSet('\\', ',', '+', '"', '<',	'>', ';');
	
	private static Map<Character, String> UNSAFE_FOR_FILTER = new HashMap<>();
	static
	{
		UNSAFE_FOR_FILTER.put('\\', "\\5c");
		UNSAFE_FOR_FILTER.put('*', "\\2a");
		UNSAFE_FOR_FILTER.put('(', "\\28");
		UNSAFE_FOR_FILTER.put(')', "\\29");
		UNSAFE_FOR_FILTER.put('\u0000', "\\00");
	}

	public static String escapeForUseAsDN(String unsafeRdnValue)
	{
		StringBuilder sb = new StringBuilder();

		if ((unsafeRdnValue.length() > 0)
				&& ((unsafeRdnValue.charAt(0) == ' ') || (unsafeRdnValue.charAt(0) == '#')))
		{
			sb.append('\\');
		}
		char[] chars = unsafeRdnValue.toCharArray();
		for (int i = 0; i < chars.length; i++)
		{
			if (UNSAFE_FOR_DN.contains(chars[i]))
				sb.append('\\');
			sb.append(chars[i]);
		}
		if ((unsafeRdnValue.length() > 1)
				&& (unsafeRdnValue.charAt(unsafeRdnValue.length() - 1) == ' '))
		{
			sb.insert(sb.length() - 1, '\\');
		}
		return sb.toString();
	}

	public static final String escapeLDAPSearchFilter(String unsafeFilterParam)
	{
		StringBuilder sb = new StringBuilder();
		char[] chars = unsafeFilterParam.toCharArray();
		
		for (int i = 0; i < chars.length; i++)
		{
			String replacement = UNSAFE_FOR_FILTER.get(chars[i]);
			sb.append(replacement == null ? chars[i] : replacement);
		}
		return sb.toString();
	}
}
