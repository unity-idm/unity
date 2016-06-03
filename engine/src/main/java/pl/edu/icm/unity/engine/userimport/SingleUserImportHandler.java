/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.userimport;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.PersistenceConfiguration;
import net.sf.ehcache.config.Searchable;
import pl.edu.icm.unity.server.api.userimport.UserImportSPI;
import pl.edu.icm.unity.server.authn.AuthenticationException;
import pl.edu.icm.unity.server.authn.AuthenticationResult;
import pl.edu.icm.unity.server.authn.remote.RemoteVerificatorUtil;
import pl.edu.icm.unity.server.authn.remote.RemotelyAuthenticatedInput;
import pl.edu.icm.unity.server.utils.CacheProvider;

/**
 * Manages imports using a single configured import facility.
 * @author K. Benedyczak
 */
public class SingleUserImportHandler
{
	private static final String CACHE_PFX = "userImportCache_";
	private UserImportSPI facility;
	private Ehcache negativeCache;
	private Ehcache positiveCache;
	private RemoteVerificatorUtil remoteUtil;
	private String translationProfile;
	private int index;
	
	
	public SingleUserImportHandler(RemoteVerificatorUtil remoteUtil, UserImportSPI facility, 
			UserImportProperties cfg,
			CacheProvider cacheProvider, int index)
	{
		this.remoteUtil = remoteUtil;
		this.facility = facility;
		this.index = index;
		this.translationProfile = cfg.getValue(UserImportProperties.TRANSLATION_PROFILE);
		this.negativeCache = getCache(cacheProvider, 
				cfg.getIntValue(UserImportProperties.NEGATIVE_CACHE), 
				CACHE_PFX + "neg_" + index);
		this.positiveCache = getCache(cacheProvider, 
				cfg.getIntValue(UserImportProperties.POSITIVE_CACHE), 
				CACHE_PFX + "pos_" + index);
	}

	private Ehcache getCache(CacheProvider cacheProvider, long ttl, String name)
	{
		CacheConfiguration cacheConfig = new CacheConfiguration(name, 0);
		Searchable searchable = new Searchable();
		searchable.values(true);
		cacheConfig.addSearchable(searchable);		
		cacheConfig.setTimeToIdleSeconds(ttl);
		cacheConfig.setTimeToLiveSeconds(ttl);
		cacheConfig.setEternal(false);
		PersistenceConfiguration persistCfg = new PersistenceConfiguration();
		persistCfg.setStrategy("none");
		cacheConfig.persistence(persistCfg);		
		return cacheProvider.getManager().addCacheIfAbsent(new Cache(cacheConfig));
	}

	
	
	public AuthenticationResult importUser(String identity, String type) throws AuthenticationException
	{
		String key = getCacheKey(identity, type);
		Element negCache = negativeCache.get(key);
		Element posCache = positiveCache.get(key);
		if (posCache != null && !posCache.isExpired())
			return null;
		if (negCache != null && !negCache.isExpired())
			return null;
		
		return doImport(key, identity, type);
	}
	
	private String getCacheKey(String identity, String type)
	{
		return type == null ? "NO_TYPE||" + identity : type + "||" + identity;
	}
	
	
	private AuthenticationResult doImport(String cacheKey, String identity, String type) 
			throws AuthenticationException
	{
		RemotelyAuthenticatedInput importedUser = facility.importUser(identity, type);
		if (importedUser == null)
		{
			negativeCache.put(new Element(cacheKey, true));
			return null;
		}
		positiveCache.put(new Element(cacheKey, true));
		return remoteUtil.getResult(importedUser, translationProfile, false);
	}

	public int getIndex()
	{
		return index;
	}
}

