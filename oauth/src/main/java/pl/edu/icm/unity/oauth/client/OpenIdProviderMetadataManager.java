/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.client;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import pl.edu.icm.unity.oauth.client.config.CustomProviderProperties;

import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.openid.connect.sdk.op.OIDCProviderMetadata;

/**
 * Manages metadata of multiple providers.
 * @author K. Benedyczak
 */
public class OpenIdProviderMetadataManager
{
	private Map<String, OpenIdConnectDiscovery> providers = new HashMap<String, OpenIdConnectDiscovery>();
	
	public void addProvider(String url) throws MalformedURLException
	{
		providers.put(url, new OpenIdConnectDiscovery(new URL(url)));
	}
	
	public OIDCProviderMetadata getMetadata(String url, CustomProviderProperties cfg) throws ParseException, IOException
	{
		return providers.get(url).getMetadata(cfg);
	}
}
