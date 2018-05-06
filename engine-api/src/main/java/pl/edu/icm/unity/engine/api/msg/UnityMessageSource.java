/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licensing information.
 */
package pl.edu.icm.unity.engine.api.msg;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;

import com.google.common.collect.Sets;

import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.authn.InvocationContext;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;

/**
 * Extension of the {@link ResourceBundleMessageSource} which 
 * automatically sets the proper locale from the {@link InvocationContext}
 * and allows for an easier invocation using varargs.
 * It also sets UTF-8 encoding and disables platform's locale fallback.
 * <p>
 * Another improvement is loading of messages from all classpath resources which are under 
 * messages/per-module-id/messages.properties path and from a respective file which is under 
 * configured i18n location in the same subdirectory as the per-module-id from classpath.
 * 
 * @author K. Benedyczak
 */
@Component
public class UnityMessageSource extends ReloadableResourceBundleMessageSource implements MessageSource
{
	private Logger log = Log.getLogger(Log.U_SERVER, UnityMessageSource.class);
	
	public static final String PROFILE_FAIL_ON_MISSING = "failOnMissingMessage";
	
	private UnityServerConfiguration config;
	private final boolean failOnMissingMessage;
	
	
	@Autowired
	public UnityMessageSource(UnityServerConfiguration config, Environment springEnv) throws IOException
	{
		Set<String> activeProfiles = Sets.newHashSet(springEnv.getActiveProfiles());
		failOnMissingMessage = activeProfiles.contains(PROFILE_FAIL_ON_MISSING); 
		init(config);
	}

	public UnityMessageSource(UnityServerConfiguration config, boolean failOnMissing) throws IOException
	{
		failOnMissingMessage = failOnMissing; 
		init(config);
	}

	private void init(UnityServerConfiguration config) throws IOException
	{
		this.config = config;
		List<String> allBundles = new ArrayList<>();
		Optional<String> fsLocation = getFSMessagesDirectory();
		
		ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
		Resource[] bundles = resolver.getResources("classpath*:messages/*/messages.properties");
		int suffixLen = ".properties".length();
		for (Resource bundle: bundles)
		{
			String classpath = bundle.getURL().toExternalForm();
			classpath = classpath.substring(0, classpath.length() - suffixLen);
			if (fsLocation.isPresent())
				allBundles.add("file:" + fsLocation.get() + getFSPathFromClasspath(classpath));
			allBundles.add(classpath);
		}
		String[] allBasenames = allBundles.toArray(new String[allBundles.size()]);
		setBasenames(allBasenames);
		setFallbackToSystemLocale(false);
		setDefaultEncoding("UTF-8");
		setAlwaysUseMessageFormat(true);
		log.debug("Messages will be loaded from the following locations: " + Arrays.toString(allBasenames));
	}
	
	private String getFSPathFromClasspath(String classpath)
	{
		int lastSlash = classpath.lastIndexOf('/');
		int oneButLastSlash = classpath.substring(0, lastSlash).lastIndexOf('/');
		return classpath.substring(oneButLastSlash + 1);
	}

	private Optional<String> getFSMessagesDirectory()
	{
		String fsMessages = config.getValue(UnityServerConfiguration.MESSAGES_DIRECTORY);
		if (fsMessages == null)
			return Optional.empty();
		return Optional.of(fsMessages.endsWith("/") ? fsMessages : fsMessages + "/");
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
			log.error("A message with code " + code + 
					" is not defined, even in a default message bundle", e);
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
