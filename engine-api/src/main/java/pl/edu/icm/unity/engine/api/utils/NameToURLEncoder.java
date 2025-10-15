/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.api.utils;

import java.util.Base64;
import java.util.Set;

public class NameToURLEncoder
{
	public static final Set<Character> UNSAFE_CHARACTERS = Set.of('/');
	public static final String ENCODED_NAME_SUFFIX = "_unc~";

	public static String encode(String name)
	{
		if (UNSAFE_CHARACTERS.stream().map(String::valueOf)
				.anyMatch(name::contains))
		{
			return Base64.getUrlEncoder()
					.encodeToString(name.getBytes()) + ENCODED_NAME_SUFFIX;
		}
		return name;
	}

	public static String decode(String name)
	{
		if (name.endsWith(ENCODED_NAME_SUFFIX))
		{
			return new String(Base64.getUrlDecoder()
					.decode(name.substring(0, name.length() - ENCODED_NAME_SUFFIX.length())));
		}
		return name;
	}
}
