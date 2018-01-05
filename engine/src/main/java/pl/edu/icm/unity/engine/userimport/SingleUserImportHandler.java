/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.userimport;

import java.util.Optional;

import org.apache.logging.log4j.Logger;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.PersistenceConfiguration;
import net.sf.ehcache.config.Searchable;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.authn.AuthenticationException;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult;
import pl.edu.icm.unity.engine.api.authn.remote.RemoteAuthnResultProcessor;
import pl.edu.icm.unity.engine.api.authn.remote.RemotelyAuthenticatedInput;
import pl.edu.icm.unity.engine.api.userimport.UserImportSPI;
import pl.edu.icm.unity.engine.api.utils.CacheProvider;
import pl.edu.icm.unity.types.basic.IdentityTaV;

/**
 * Manages imports using a single configured import facility.
 * @author K. Benedyczak
 */
public class SingleUserImportHandler
{
	private static final Logger log = Log.getLogger(Log.U_SERVER,
			SingleUserImportHandler.class);
	private static final String CACHE_PFX = "userImportCache_";
	private UserImportSPI facility;
	private Ehcache negativeCache;
	private Ehcache positiveCache;
	private RemoteAuthnResultProcessor remoteUtil;
	private String translationProfile;
	
	public SingleUserImportHandler(RemoteAuthnResultProcessor remoteUtil, UserImportSPI facility, 
			UserImportProperties cfg,
			CacheProvider cacheProvider, String key)
	{
		this.remoteUtil = remoteUtil;
		this.facility = facility;
		this.translationProfile = cfg.getValue(UserImportProperties.TRANSLATION_PROFILE);
		this.negativeCache = getCache(cacheProvider, 
				cfg.getIntValue(UserImportProperties.NEGATIVE_CACHE), 
				CACHE_PFX + "neg_" + key);
		this.positiveCache = getCache(cacheProvider, 
				cfg.getIntValue(UserImportProperties.POSITIVE_CACHE), 
				CACHE_PFX + "pos_" + key);
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

	
	
	public AuthenticationResult importUser(String identity, String type, 
			Optional<IdentityTaV> existingUser) throws AuthenticationException
	{
		String key = getCacheKey(identity, type);
		Element negCache = negativeCache.get(key);
		Element posCache = positiveCache.get(key);
		if (posCache != null && !posCache.isExpired())
		{
			log.debug("Returning cached positive import result for {}", identity);
			return (AuthenticationResult) posCache.getObjectValue();
		}
		if (negCache != null && !negCache.isExpired())
		{
			log.debug("Returning cached negative import result for {}", identity);
			return null;
		}
		
		return doImport(key, identity, type, existingUser);
	}
	
	private String getCacheKey(String identity, String type)
	{
		return type == null ? "NO_TYPE||" + identity : type + "||" + identity;
	}
	
	
	private AuthenticationResult doImport(String cacheKey, String identity, String type, 
			Optional<IdentityTaV> existingUser) 
			throws AuthenticationException
	{
		RemotelyAuthenticatedInput importedUser = facility.importUser(identity, type);
		if (importedUser == null)
		{
			log.debug("Caching negative import result for {}", identity);
			negativeCache.put(new Element(cacheKey, true));
			return null;
		}
		log.debug("Caching positive import result for {}", identity);
		AuthenticationResult result = remoteUtil.getResult(importedUser, 
				translationProfile, false, existingUser);
		positiveCache.put(new Element(cacheKey, result));
		return result;
	}
}

