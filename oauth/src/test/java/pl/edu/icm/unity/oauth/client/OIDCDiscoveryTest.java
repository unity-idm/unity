/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.oauth.client;

import java.io.IOException;
import java.net.URL;
import java.util.Properties;

import org.junit.Assert;
import org.junit.Test;

import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.openid.connect.sdk.op.OIDCProviderMetadata;

import pl.edu.icm.unity.oauth.client.OpenIdConnectDiscovery;
import pl.edu.icm.unity.oauth.client.config.CustomProviderProperties;

public class OIDCDiscoveryTest
{
	@Test
	public void test() throws ParseException, IOException
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
		Assert.assertEquals("accounts.google.com", meta.getIssuer().getValue());
	}
}
