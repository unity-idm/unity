/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.oauth.client;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URL;
import java.util.Properties;

import org.junit.Assert;
import org.junit.Test;

import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.http.HTTPRequest;
import com.nimbusds.oauth2.sdk.http.HTTPResponse;
import com.nimbusds.openid.connect.sdk.op.OIDCProviderMetadata;

import pl.edu.icm.unity.oauth.client.config.CustomProviderProperties;

public class OIDCDiscoveryTest
{
	@Test
	public void shouldDiscoverMetadataFromGoogle() throws ParseException, IOException
	{
		OpenIdConnectDiscovery tested = new OpenIdConnectDiscovery(new URL(
				"https://accounts.google.com/.well-known/openid-configuration"));
		Properties props = new Properties();
		props.setProperty("translationProfile", "");
		props.setProperty("clientSecret", "");
		props.setProperty("clientId", "");
		props.setProperty("name", "");
		props.setProperty("openIdConnect", "true");
		props.setProperty("openIdConnectDiscoveryEndpoint", "https://accounts.google.com/.well-known/openid-configuration");
		CustomProviderProperties def = new CustomProviderProperties(props, "", null);
		OIDCProviderMetadata meta = tested.getMetadata(def);
		Assert.assertEquals("https://accounts.google.com", meta.getIssuer().getValue());
	}

	private final String META = "{"
			+ "\"issuer\": \"https://mock-issuer\","
			+ "\"authorization_endpoint\": \"https://mock-authz\","
			+ "\"token_endpoint\": \"https://mock-token\","
			+ "\"jwks_uri\": \"https://mock-certs\","
			+ "\"subject_types_supported\": [\"public\"]"
			+ "}";
	
	@Test
	public void shouldDiscoverMetadataWithoutCacheHeaders() throws ParseException, IOException
	{
		CustomHttpRequestFactory reqFactory = mock(CustomHttpRequestFactory.class);
		CustomProviderProperties def = mock(CustomProviderProperties.class);
		URL url = new URL("https://test.org");
		HTTPRequest wrapped =  mock(HTTPRequest.class);
		HTTPResponse response = mock(HTTPResponse.class);
		when(reqFactory.wrapRequest(any(), any())).thenReturn(wrapped);
		when(wrapped.send()).thenReturn(response);
		when(response.getCacheControl()).thenReturn(null);
		when(response.getContent()).thenReturn(META);
		OpenIdConnectDiscovery tested = new OpenIdConnectDiscovery(url,	reqFactory);
		
		OIDCProviderMetadata meta = tested.getMetadata(def);
		
		Assert.assertEquals("https://mock-issuer", meta.getIssuer().getValue());
	}
}
