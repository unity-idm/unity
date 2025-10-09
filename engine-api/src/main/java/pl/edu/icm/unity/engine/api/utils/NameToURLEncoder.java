/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.api.utils;

import java.util.Base64;
import java.util.Set;

public class NameToURLEncoder
{
	public static final Set<String> FORBIDDEN = Set.of("/");
	public static final String suffix = "_unc~";

	public static String encode(String name)
	{
		if (FORBIDDEN.stream()
				.anyMatch(name::contains))
		{
			return Base64.getUrlEncoder()
					.encodeToString(name.getBytes()) + suffix;
		}
		return name;
	}

	public static String decode(String name)
	{
		if (name.endsWith(suffix))
		{
			return new String(Base64.getUrlDecoder()
					.decode(name.substring(0, name.length() - suffix.length())));
		}
		return name;
	}
}
