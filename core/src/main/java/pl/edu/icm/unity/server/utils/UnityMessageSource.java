/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licensing information.
 */
package pl.edu.icm.unity.server.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.server.authn.InvocationContext;

/**
 * Extension of the {@link ResourceBundleMessageSource} which 
 * automatically sets the proper locale from the {@link InvocationContext}
 * and allows for an easier invocation using varargs.
 * It also sets UTF-8 encoding and disables platform's locale fallback.
 * @author K. Benedyczak
 */
@Component
public class UnityMessageSource extends ResourceBundleMessageSource implements MessageSource
{
	private UnityServerConfiguration config;

	@Autowired
	public UnityMessageSource(UnityServerConfiguration config,
			List<UnityMessageBundles> bundles)
	{
		super();
		this.config = config;
		List<String> allBundles = new ArrayList<String>();
		for (UnityMessageBundles bundle: bundles)
			allBundles.addAll(bundle.getBundles());
		setBasenames(allBundles.toArray(new String[allBundles.size()]));
		setFallbackToSystemLocale(false);
		setDefaultEncoding("UTF-8");
	}
	
	@Override
	public String getMessage(String code, Object... args)
	{
		Locale loc = getLocale();
		return super.getMessage(code, args, loc);
	}

	/**
	 * If any of the arguments is null, then empty string is returned
	 * @param code
	 * @param args
	 * @return
	 */
	@Override
	public String getMessageNullArg(String code, Object... args)
	{
		for (Object a: args)
			if (a == null)
				return "";
		return getMessage(code, args);
	}
	
	@Override
	public String getYesNo(boolean value)
	{
		return value ? getMessage("yes") : getMessage("no");
	}
	
	@Override
	public Locale getLocale()
	{
		return getLocale(config.getDefaultLocale());
	}
	
	public Map<String, Locale> getEnabledLocales()
	{
		return config.getEnabledLocales();
	}

	@Override
	public String getDefaultLocaleCode()
	{
		return config.getDefaultLocale().toString();
	}

	@Override
	public String getLocaleCode()
	{
		return getLocale().toString();
	}
	
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
