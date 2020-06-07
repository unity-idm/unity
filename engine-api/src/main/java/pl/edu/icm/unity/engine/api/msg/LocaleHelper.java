/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */


package pl.edu.icm.unity.engine.api.msg;

import java.util.Locale;

import pl.edu.icm.unity.engine.api.authn.InvocationContext;

public class LocaleHelper
{
	public static Locale getLocale(Locale fallback)
	{	
		InvocationContext ctx = null;
		try
		{
			ctx = InvocationContext.getCurrent();
		} catch (Exception e)
		{
			return fallback;
		}
		Locale ret = ctx.getLocale();
		if (ret == null)
			return fallback;
		return ret;
	}
}
