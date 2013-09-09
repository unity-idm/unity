/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.notifications;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.server.utils.UnityServerConfiguration;

/**
 * Loads message templates from a simple properties file.
 * <p>
 * The scheme: 
 *  TEMPLATEID.body=body template
 *  TEMPLATEID.subject=subject template
 *  
 * also it is possible to create locale specific entries:
 *  TEMPLATEID.body.fr=body template in French
 *  
 * @author K. Benedyczak
 */
public class TemplatesStore
{
	private Properties properties;
	private Locale defaultLocale;
	private Map<String, NotificationTemplate> cachedTemplates;
	
	public TemplatesStore(Properties properties, Locale defaultLocale)
	{
		this.properties = properties;
		this.defaultLocale = defaultLocale;
		cachedTemplates = new ConcurrentHashMap<>(10);
	}
	
	public NotificationTemplate getTemplate(String id) throws WrongArgumentException
	{
		NotificationTemplate ret = cachedTemplates.get(id);
		if (ret != null)
			return ret;
		ret = loadTemplate(id);
		cachedTemplates.put(id, ret);
		return ret;
	}
	
	private NotificationTemplate loadTemplate(String id) throws WrongArgumentException
	{
		String body = properties.getProperty(id+".body");
		String subject = properties.getProperty(id+".subject");
		if (body == null || subject == null)
			throw new WrongArgumentException("There is no template for this id");
		Map<Locale, String> bodies = new HashMap<>();
		bodies.put(defaultLocale, body);
		Map<Locale, String> subjects = new HashMap<>();
		subjects.put(defaultLocale, subject);
		
		Set<Object> keys = properties.keySet();
		for (Object keyO: keys)
		{
			String key = (String) keyO;
			processKey(key, id+".body.", subjects);
			processKey(key, id+".subject.", subjects);
		}
		return new NotificationTemplate(bodies, subjects, defaultLocale);
	}
	
	private void processKey(String key, String pfx, Map<Locale, String> map)
	{
		if (key.startsWith(pfx))
		{
			String locale = key.substring(pfx.length());
			Locale l = UnityServerConfiguration.safeLocaleDecode(locale);
			map.put(l, properties.getProperty(key));
		}
	}
}
