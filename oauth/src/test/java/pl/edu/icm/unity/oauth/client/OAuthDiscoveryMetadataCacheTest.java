/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.oauth.client;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.Properties;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.openid.connect.sdk.op.OIDCProviderMetadata;

import pl.edu.icm.unity.engine.api.idp.CommonIdPProperties;
import pl.edu.icm.unity.oauth.client.config.CustomProviderProperties;

@RunWith(MockitoJUnitRunner.class)
public class OAuthDiscoveryMetadataCacheTest
{
	@Test
	public void shouldCacheMeta() throws ParseException, IOException, URISyntaxException
	{
		OpenIdConnectDiscovery mockDownloader = mock(OpenIdConnectDiscovery.class);
		when(mockDownloader.getMetadata(any(), any())).thenReturn(mock(OIDCProviderMetadata.class));
		
		OAuthDiscoveryMetadataCache cache = new OAuthDiscoveryMetadataCache(mockDownloader,
				Duration.ofMinutes(1));
		cache.clear();
		Properties props = new Properties();
		props.setProperty(CustomProviderProperties.OPENID_DISCOVERY,
				"https://accounts.google.com/.well-known/openid-configuration");
		props.setProperty(CustomProviderProperties.CLIENT_HOSTNAME_CHECKING, "warn");
		props.setProperty(CommonIdPProperties.TRANSLATION_PROFILE, "");
		props.setProperty(CustomProviderProperties.CLIENT_SECRET, "");
		props.setProperty(CustomProviderProperties.CLIENT_ID, "");
		props.setProperty(CustomProviderProperties.PROVIDER_NAME, "");
		props.setProperty(CustomProviderProperties.OPENID_CONNECT, "true");
		CustomProviderProperties def = new CustomProviderProperties(props, "", null);
		cache.getMetadata(def);
		cache.getMetadata(def);

		verify(mockDownloader, times(1)).getMetadata(eq("https://accounts.google.com/.well-known/openid-configuration"),
				eq(def));
	}

	@Test
	public void shouldCacheAccordingToTtl() throws ParseException, IOException, URISyntaxException, InterruptedException
	{
		OpenIdConnectDiscovery mockDownloader = mock(OpenIdConnectDiscovery.class);
		when(mockDownloader.getMetadata(any(), any())).thenReturn(mock(OIDCProviderMetadata.class));
		OAuthDiscoveryMetadataCache cache = new OAuthDiscoveryMetadataCache(mockDownloader,
				Duration.ofMillis(20));
		cache.clear();	
		Properties props = new Properties();
		props.setProperty(CustomProviderProperties.OPENID_DISCOVERY,
				"https://accounts.google.com/.well-known/openid-configuration");
		props.setProperty(CustomProviderProperties.CLIENT_HOSTNAME_CHECKING, "warn");
		props.setProperty(CommonIdPProperties.TRANSLATION_PROFILE, "");
		props.setProperty(CustomProviderProperties.CLIENT_SECRET, "");
		props.setProperty(CustomProviderProperties.CLIENT_ID, "");
		props.setProperty(CustomProviderProperties.PROVIDER_NAME, "");
		props.setProperty(CustomProviderProperties.OPENID_CONNECT, "true");
		CustomProviderProperties def = new CustomProviderProperties(props, "", null);
		cache.getMetadata(def);
		Thread.sleep(21);
		cache.getMetadata(def);

		verify(mockDownloader, times(2)).getMetadata(eq("https://accounts.google.com/.well-known/openid-configuration"),
				eq(def));
	}
}
