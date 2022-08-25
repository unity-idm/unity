/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.userimport;

import java.time.Duration;
import java.util.Optional;

import org.apache.logging.log4j.Logger;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult;
import pl.edu.icm.unity.engine.api.authn.RemoteAuthenticationException;
import pl.edu.icm.unity.engine.api.authn.remote.RemoteAuthnResultTranslator;
import pl.edu.icm.unity.engine.api.authn.remote.RemotelyAuthenticatedInput;
import pl.edu.icm.unity.engine.api.userimport.UserImportSPI;
import pl.edu.icm.unity.types.basic.IdentityTaV;

/**
 * Manages imports using a single configured import facility.
 */
class SingleUserImportHandler
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_USER_IMPORT,
			SingleUserImportHandler.class);
	private UserImportSPI facility;
	private Cache<String, Boolean> negativeCache;
	private Cache<String, AuthenticationResult> positiveCache;
	private RemoteAuthnResultTranslator remoteUtil;
	private String translationProfile;
	
	SingleUserImportHandler(RemoteAuthnResultTranslator remoteUtil, UserImportSPI facility, 
			UserImportProperties cfg)
	{
		this.remoteUtil = remoteUtil;
		this.facility = facility;
		this.translationProfile = cfg.getValue(UserImportProperties.TRANSLATION_PROFILE);
		this.negativeCache = CacheBuilder.newBuilder()
				.expireAfterWrite(Duration.ofSeconds(cfg.getIntValue(UserImportProperties.NEGATIVE_CACHE)))
				.build(); 
		this.positiveCache = CacheBuilder.newBuilder()
				.expireAfterWrite(Duration.ofSeconds(cfg.getIntValue(UserImportProperties.POSITIVE_CACHE)))
				.build(); 
	}

	AuthenticationResult importUser(String identity, String type, 
			Optional<IdentityTaV> existingUser) throws RemoteAuthenticationException
	{
		String key = getCacheKey(identity, type);
		Boolean negCache = negativeCache.getIfPresent(key);
		AuthenticationResult posCache = positiveCache.getIfPresent(key);
		if (posCache != null)
		{
			log.debug("Returning cached positive import result for {}", identity);
			return posCache;
		}
		if (negCache != null)
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
			throws RemoteAuthenticationException
	{
		RemotelyAuthenticatedInput importedUser = facility.importUser(identity, type);
		if (importedUser == null)
		{
			log.debug("Caching negative import result for {}", identity);
			negativeCache.put(cacheKey, true);
			return null;
		}
		log.debug("Caching positive import result for {}", identity);
		AuthenticationResult result = remoteUtil.getTranslatedResult(importedUser, 
				translationProfile, false, existingUser, null, false);
		positiveCache.put(cacheKey, result);
		return result;
	}
}

