/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.stdext.credential.sms;

import java.time.Duration;

import org.springframework.stereotype.Component;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import pl.edu.icm.unity.engine.api.authn.AuthenticationSubject;

/**
 * Authn sms limit counter. Used by {@link SMSVerificator} to check how many authn sms have been sent to user
 */
@Component
class AuthnSMSCounter
{
	private final Cache<AuthenticationSubject, Integer> smsReqCache;
	
	AuthnSMSCounter()
	{
		smsReqCache = CacheBuilder.newBuilder()
				.expireAfterWrite(Duration.ofHours(48))
				.build();
	}

	synchronized void incValue(AuthenticationSubject username)
	{
		Integer lastValue = smsReqCache.getIfPresent(username);
		int value = lastValue != null ? lastValue + 1 : 1;
		smsReqCache.put(username, value);
	}

	synchronized boolean reset(AuthenticationSubject username)
	{
		boolean wasPresent = smsReqCache.getIfPresent(username) != null;
		smsReqCache.invalidate(username);
		return wasPresent;
	}
	
	synchronized int getValue(AuthenticationSubject username)
	{
		Integer value = smsReqCache.getIfPresent(username);
		return value == null ? 0 : value;
	}
}
