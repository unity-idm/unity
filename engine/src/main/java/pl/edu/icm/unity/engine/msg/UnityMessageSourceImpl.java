/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licensing information.
 */
package pl.edu.icm.unity.engine.msg;

import com.google.common.collect.Sets;
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
import pl.edu.icm.unity.base.i18n.I18nString;
import pl.edu.icm.unity.base.message.MessageArea;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.authn.InvocationContext;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.engine.api.msg.LocaleHelper;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.*;

/**
 * Extension of the {@link ResourceBundleMessageSource} which 
 * automatically sets the proper locale from the {@link InvocationContext}
 * and allows for an easier invocation using varargs.
 * It also sets UTF-8 encoding and disables platform's locale fallback.
 * <p>
 * Improvements: 
 * 1. Loading of messages from all classpath resources which are under 
 * messages/per-module-id/messages.properties path and from a respective file which is under 
 * configured i18n location in the same subdirectory as the per-module-id from classpath.
 * 2. Loading of messages from db from table messages. 
 * 
 * @author K. Benedyczak
 * @author P.Piernik
 */
@Component
public class UnityMessageSourceImpl extends ReloadableResourceBundleMessageSource implements MessageSource
{
	private Logger log = Log.getLogger(Log.U_SERVER_CORE, UnityMessageSourceImpl.class);
	
	private UnityServerConfiguration config;
	private final boolean failOnMissingMessage;
	private Map<MessageArea, Set<Object>> byCategory;
	private MessageRepository msgRep;
	private MessageAreaRegistry areas;
	

	@Autowired
	public UnityMessageSourceImpl(UnityServerConfiguration config, Environment springEnv, MessageRepository msgMan,
			MessageAreaRegistry areas) throws IOException
	{
		this.msgRep = msgMan;
		this.areas = areas;
		Set<String> activeProfiles = Sets.newHashSet(springEnv.getActiveProfiles());
		failOnMissingMessage = activeProfiles.contains(PROFILE_FAIL_ON_MISSING); 
		init(config);
	}

	public UnityMessageSourceImpl(UnityServerConfiguration config, boolean failOnMissing) throws IOException
	{
		failOnMissingMessage = failOnMissing; 
		init(config);
	}

	private void init(UnityServerConfiguration config) throws IOException
	{
		byCategory = new HashMap<>();
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
		log.info("Messages will be loaded from the following locations: " + Arrays.toString(allBasenames));
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
		return LocaleHelper.getLocale(config.getDefaultLocale());
	}

	@Override
	public Locale getLocaleForTimeFormat()
	{
		Locale locale = getLocale();
		if(locale.getLanguage().equals("en"))
			return Locale.UK;
		return locale;
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

	@Override
	public Map<String, Locale> getSupportedLocales()
	{
		return UnityServerConfiguration.SUPPORTED_LOCALES;
	}
	
	public Map<MessageArea, Set<Object>> getKeysByCategory()
	{
		return byCategory;
	}
	
	@Override
	protected Properties loadProperties(Resource resource, String fileName) throws IOException
	{
		log.trace("Load message file"  + fileName);
		int lastSlash = fileName.lastIndexOf('/');
		int oneButLastSlash = fileName.substring(0, lastSlash).lastIndexOf('/');
		String category =  fileName.substring(oneButLastSlash +  1, lastSlash);
		MessageArea area = null;
		try
		{
			area = areas.getByName(category).getMessageArea();
		} catch (IllegalArgumentException e)
		{
			area = new MessageArea(category, "UnknownMessageArea.displayedName", false);
		}
		Properties ret =  super.loadProperties(resource, fileName);
		if (byCategory.containsKey(area)) 
		{
			Set<Object> arSet = byCategory.get(area);
			ret.keySet().forEach(c -> arSet.add(c));
		}else {
			byCategory.put(area, new HashSet<>(ret.keySet()));
		}
		
		return ret;	
	}

	@Override
	public I18nString getI18nMessage(String code, Object... args)
	{
		I18nString ret = new I18nString();
		for (Locale locale : getEnabledLocales().values())
		{
			String msg = "";
			try {
				 msg = super.getMessage(code, args, locale);
			}catch (NoSuchMessageException e)
			{
				//ok
			}
			ret.addValue(locale.toString(), msg);
		}
		return ret;	
	}

	@Override
	protected String getMessageInternal(String code, Object[] args, Locale locale)
	{
		Object[] argsToUse = args;
		Optional<String> fromDB = msgRep.get(code, locale);
		
		if (fromDB.isPresent())
		{
			argsToUse = resolveArguments(args, locale);
			MessageFormat format = new MessageFormat(fromDB.get(), locale);
			return format.format(argsToUse);
		}
		
		return super.getMessageInternal(code, args, locale);
	}
}
