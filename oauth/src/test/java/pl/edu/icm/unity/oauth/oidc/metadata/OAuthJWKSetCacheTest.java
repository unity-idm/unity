/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.oauth.oidc.metadata;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URISyntaxException;
import java.time.Duration;

import org.junit.Test;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.oauth2.sdk.ParseException;

import eu.unicore.util.httpclient.ServerHostnameCheckingMode;

public class OAuthJWKSetCacheTest
{
	@Test
	public void shouldCacheMeta() throws ParseException, IOException, URISyntaxException, java.text.ParseException
	{
		KeyResource mockDownloader = mock(KeyResource.class);
		when(mockDownloader.getJWKSet(any())).thenReturn(mock(JWKSet.class));

		OAuthJWKSetCache cache = new OAuthJWKSetCache(mockDownloader, Duration.ofMinutes(1));
		cache.clear();
		JWKSetRequest request = JWKSetRequest.builder()
				.withHostnameChecking(ServerHostnameCheckingMode.NONE)
				.withUrl("https://mock")
				.build();

		cache.getMetadata(request);
		cache.getMetadata(request);

		verify(mockDownloader, times(1)).getJWKSet(eq(request));
	}
}
