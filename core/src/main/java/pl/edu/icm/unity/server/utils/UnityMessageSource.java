/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licensing information.
 */
package pl.edu.icm.unity.server.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.server.authn.InvocationContext;

import com.google.common.collect.Sets;

/**
 * Extension of the {@link ResourceBundleMessageSource} which 
 * automatically sets the proper locale from the {@link InvocationContext}
 * and allows for an easier invocation using varargs.
 * It also sets UTF-8 encoding and disables platform's locale fallback.
 * @author K. Benedyczak
 */
@Component
public class UnityMessageSource extends ReloadableResourceBundleMessageSource implements MessageSource
{
	private Logger log = Log.getLogger(Log.U_SERVER, UnityMessageSource.class);
	
	public static final String PROFILE_FAIL_ON_MISSING = "failOnMissingMessage";
	
	private static final String DEFAULT_PACKAGE = "pl/edu/icm/unity/"; 
	private UnityServerConfiguration config;
	private final boolean failOnMissingMessage;
	
	
	@Autowired
	public UnityMessageSource(UnityServerConfiguration config,
			List<UnityMessageBundles> bundles, Environment springEnv)
	{
		super();
		this.config = config;
		List<String> allBundles = new ArrayList<String>();
		String fsLocation = getFSMessagesDirectory();
		for (UnityMessageBundles bundle: bundles)
			bundle.getBundles().forEach(src -> {
				allBundles.add("file:" + fsLocation + shortenPath(src));
				allBundles.add("classpath:" + src);
			});
		
		setBasenames(allBundles.toArray(new String[allBundles.size()]));
		setFallbackToSystemLocale(false);
		setDefaultEncoding("UTF-8");
		setAlwaysUseMessageFormat(true);
		Set<String> activeProfiles = Sets.newHashSet(springEnv.getActiveProfiles());
		failOnMissingMessage = activeProfiles.contains(PROFILE_FAIL_ON_MISSING);
	}
	
	private String shortenPath(String fullPath)
	{
		return fullPath.startsWith(DEFAULT_PACKAGE) ? 
				fullPath.substring(DEFAULT_PACKAGE.length()) : 
				fullPath;
	}
	
	private String getFSMessagesDirectory()
	{
		String fsMessages = config.getValue(UnityServerConfiguration.MESSAGES_DIRECTORY);
		return fsMessages.endsWith("/") ? fsMessages : fsMessages + "/";
	}

	@Override
	public String getMessageUnsafe(String code, Object... args)
	{
		Locale loc = getLocale();
		return super.getMessage(code, args, loc);
	}
	
	@Override
	public String getMessage(String code, Object... args)
	{
		if (failOnMissingMessage)
			return getMessageUnsafe(code, args);
		
		Locale loc = getLocale();
		try
		{
			return super.getMessage(code, args, loc);
		} catch (NoSuchMessageException e)
		{
			log.error("A message with code " + code + " is not defined, even in a default message bundle", e);
			return code;
		}
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
	
	@Override
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

	@Override
	public Map<String, Locale> getSupportedLocales()
	{
		return UnityServerConfiguration.SUPPORTED_LOCALES;
	}
}
