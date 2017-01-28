/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.utils;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import eu.unicore.util.configuration.ConfigurationException;
import eu.unicore.util.configuration.PropertiesHelper;
import eu.unicore.util.configuration.PropertyMD;
import pl.edu.icm.unity.types.I18nString;

/**
 * Extends {@link PropertiesHelper} with Unity specific methods: returning localized strings
 * and caching of {@link #getSortedStringKeys(String, boolean)} method results.
 * 
 * @author K. Benedyczak
 */
public class UnityPropertiesHelper extends PropertiesHelper
{
	private Set<Pattern> cachedPrefixes = new HashSet<>();
	protected Map<String, Set<String>> listKeysCache = new HashMap<>();
	
	public UnityPropertiesHelper(String prefix, Properties properties,
			Map<String, PropertyMD> propertiesMD, Logger log)
			throws ConfigurationException
	{
		super(prefix, properties, propertiesMD, log);
	}
	
	/**
	 * For cloning
	 * @param cloned
	 */
	protected UnityPropertiesHelper(UnityPropertiesHelper cloned)
	{
		super(cloned);
		this.cachedPrefixes.addAll(cloned.cachedPrefixes);
		this.listKeysCache.putAll(cloned.listKeysCache);
	}
	
	/**
	 * All properties which start with the given regexp will be cached in a way that 
	 * all properties starting with this prefix can be quickly retrieved using 
	 * {@link #getSortedStringKeys(String, boolean)}. 
	 * This also optimizes uses of {@link #getListOfValues(String)} which relies on the above methods.
	 * Note that the regexp must include the main prefix of this properties object.
	 * @param prefixRegexp
	 */
	public void addCachedPrefixes(String... prefixRegexp)
	{
		for (String prefix: prefixRegexp)
			cachedPrefixes.add(Pattern.compile(prefix));
		refillCache();
	}
	
	@Override
	public synchronized void setProperties(Properties properties) throws ConfigurationException
	{
		super.setProperties(properties);
		refillCache();
	}

	@Override
	public synchronized void setProperty(String key, String value)
	{
		super.setProperty(key, value);
		cacheIfNeeded(prefix+key);
	}
	
	protected void refillCache()
	{
		listKeysCache.clear();
		for (Object key: properties.keySet())
			cacheIfNeeded((String) key);
	}
	
	protected void cacheIfNeeded(String key)
	{
		for (Pattern pattern: cachedPrefixes)
		{
			Matcher matcher = pattern.matcher(key);
			if (matcher.find() && matcher.start() == 0)
			{
				String matchedPfx = matcher.group();
				Set<String> set = listKeysCache.get(matchedPfx);
				if (set == null)
				{
					set = new HashSet<>();
					listKeysCache.put(matchedPfx, set);
				}
				set.add(key);
			}
		}
	}

	protected boolean isCached(String key)
	{
		if (cachedPrefixes == null)
			return false;
		for (Pattern pattern: cachedPrefixes)
		{
			Matcher matcher = pattern.matcher(key);
			if (matcher.find() && matcher.start() == 0)
				return true;
		}
		return false;
	}
	
	@Override
	protected synchronized Set<String> getSortedStringKeys(String base, boolean allowListSubKeys)
	{
		SortedSet<String> keys = new TreeSet<String>();
		
		Set<?> allKeys = isCached(base) ? listKeysCache.get(base) : properties.keySet();
		if (allKeys == null)
			return Collections.emptySet();
		for (Object keyO: allKeys)
		{
			String key = (String) keyO;
			if (key.startsWith(base))
			{
				String post = key.substring(base.length());
				int dot = post.indexOf('.');
				if (dot != -1 && allowListSubKeys)
					post = post.substring(0, dot);
				else if (dot != -1 && !allowListSubKeys)
				{
					log.warn("Property list key '" + key + 
							"' should not posses a dot: '" +
							post + "'. Ignoring.");
					continue;
				}
					
				keys.add(base+post);
			}
		}
		return keys;
	}
	
	/**
	 * @param msg
	 * @param baseKey Property name (without the prefix).
	 * @return localized string of the given property. Default value is set to the regular value of the property.
	 * All sub values of the base key are used as localized values, assuming their subkey is equal to one of
	 * supported locales.
	 */
	public I18nString getLocalizedString(UnityMessageSource msg, String baseKey)
	{
		return getLocalizedString(this, msg, baseKey);
	}
	

	/**
	 * @param msg
	 * @param baseKey Property name (without the prefix).
	 * @return localized string of the given property. Default value is set to the regular value of the property.
	 * All sub values of the base key are used as localized values, assuming their subkey is equal to one of
	 * supported locales.
	 */
	static I18nString getLocalizedString(PropertiesHelper helper, UnityMessageSource msg, String baseKey)
	{
		I18nString ret = new I18nString();
		Map<String, Locale> supportedLocales = msg.getSupportedLocales();
		String defaultVal = helper.getValue(baseKey);
		if (defaultVal != null)
			ret.setDefaultValue(defaultVal);
		for (Map.Entry<String, Locale> locale: supportedLocales.entrySet())
		{
			String v = helper.getLocalizedValue(baseKey, locale.getValue());
			if (v != null && !v.equals(defaultVal))
				ret.addValue(locale.getKey(), v);
		}
		return ret;
	}
}
