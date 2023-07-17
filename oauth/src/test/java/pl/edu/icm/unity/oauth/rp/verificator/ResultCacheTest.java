/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.rp.verificator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.util.Date;

import org.junit.jupiter.api.Test;

import com.nimbusds.oauth2.sdk.Scope;

import pl.edu.icm.unity.oauth.client.AttributeFetchResult;

public class ResultCacheTest
{
	@Test
	public void shouldNotCacheWhenCacheDisabled()
	{
		ResultsCache cache = new ResultsCache(0);
		
		cache.cache("id", new TokenStatus(true, new Date(System.currentTimeMillis() + 10000), 
				Scope.parse("email"), "subject"), mock(AttributeFetchResult.class));
		
		assertThat(cache.getCached("id")).isNull();
	}


	@Test
	public void shouldNotReturnExpiredTokenWhenNoGlobalCacheConfigured()
	{
		ResultsCache cache = new ResultsCache(-1);
		
		cache.cache("id", new TokenStatus(true, new Date(System.currentTimeMillis() - 10000), 
				Scope.parse("email"), "subject"), mock(AttributeFetchResult.class));
		
		assertThat(cache.getCached("id")).isNull();
	}

	@Test
	public void shouldReturnNotExpiredTokenWhenNoGlobalCacheConfigured()
	{
		ResultsCache cache = new ResultsCache(-1);
		
		TokenStatus status = new TokenStatus(true, new Date(System.currentTimeMillis() + 10000), 
				Scope.parse("email"), "subject");
		
		cache.cache("id", status, mock(AttributeFetchResult.class));
		
		assertThat(cache.getCached("id")).isNotNull();
		assertThat(cache.getCached("id").getTokenStatus()).isEqualTo(status);
	}

	@Test
	public void shouldReturnNotExpiredTokenWhenGlobalCacheConfigured()
	{
		ResultsCache cache = new ResultsCache(1000);
		
		TokenStatus status = new TokenStatus(true, new Date(System.currentTimeMillis() + 10000), 
				Scope.parse("email"), "subject");
		
		cache.cache("id", status, mock(AttributeFetchResult.class));
		
		assertThat(cache.getCached("id")).isNotNull();
		assertThat(cache.getCached("id").getTokenStatus()).isEqualTo(status);
	}

	@Test
	public void shouldNotReturnExpiredTokenWhenGlobalCacheConfigured()
	{
		ResultsCache cache = new ResultsCache(1000);
		
		cache.cache("id", new TokenStatus(true, new Date(System.currentTimeMillis() - 10000), 
				Scope.parse("email"), "subject"), mock(AttributeFetchResult.class));
		
		assertThat(cache.getCached("id")).isNull();
	}
	
}
