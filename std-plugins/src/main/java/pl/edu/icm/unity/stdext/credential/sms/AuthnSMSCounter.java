/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.stdext.credential.sms;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.PersistenceConfiguration;
import net.sf.ehcache.config.Searchable;
import pl.edu.icm.unity.engine.api.utils.CacheProvider;

/**
 * Authn sms limit counter. Used by {@link SMSVerificator} to check how many authn sms have been sent to user
 * @author P.Piernik
 *
 */
@Component
public class AuthnSMSCounter
{
	private static final String CACHE_ID = "AuthnSMSCounter";
	private Ehcache smsReqCache;
	
	@Autowired
	public AuthnSMSCounter(CacheProvider cacheProvider)
	{

		CacheConfiguration cacheConfig = new CacheConfiguration(CACHE_ID, 0);
		Searchable searchable = new Searchable();
		searchable.values(true);
		cacheConfig.addSearchable(searchable);
		cacheConfig.setTimeToIdleSeconds(48 * 3600);
		cacheConfig.setEternal(false);
		PersistenceConfiguration persistCfg = new PersistenceConfiguration();
		persistCfg.setStrategy("none");
		cacheConfig.persistence(persistCfg);
		smsReqCache = cacheProvider.getManager().addCacheIfAbsent(new Cache(cacheConfig));
	}

	public synchronized void incValue(String username)
	{
		Element element = smsReqCache.get(username);
		int value = 1;
		if (element != null && element.getObjectValue() != null)
		{
			int old = Integer.parseInt(element.getObjectValue().toString());
			value = old + 1;
		}
		smsReqCache.put(new Element(username, value));
	}

	public synchronized boolean reset(String username)
	{
		return smsReqCache.remove(username);
	}
	
	public synchronized int getValue(String username)
	{
		Element element = smsReqCache.get(username);
		if (element != null && element.getObjectValue() != null)
		{
			return Integer.parseInt(element.getObjectValue().toString());
		}
		return 0;
	}
}
