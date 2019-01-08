/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.client;

import java.io.IOException;
import java.net.URL;
import java.util.StringTokenizer;

import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.http.HTTPRequest;
import com.nimbusds.oauth2.sdk.http.HTTPRequest.Method;
import com.nimbusds.oauth2.sdk.http.HTTPResponse;
import com.nimbusds.openid.connect.sdk.op.OIDCProviderMetadata;

import pl.edu.icm.unity.oauth.client.config.CustomProviderProperties;

/**
 * Retrieves and caches information about OpenID Connect provider.
 *  
 * @author K. Benedyczak
 */
public class OpenIdConnectDiscovery
{
	private static final long DEFAULT_MAX_AGE = 30000;
	private URL providerMetadataEndpoint;
	private OIDCProviderMetadata providerMeta;
	private long expiresAt = -1;
	private CustomHttpRequestFactory requestFactory;
	
	public OpenIdConnectDiscovery(URL providerMetadataEndpoint)
	{
		this(providerMetadataEndpoint, new CustomHttpRequestFactory());
	}

	OpenIdConnectDiscovery(URL providerMetadataEndpoint, CustomHttpRequestFactory requestFactory)
	{
		this.providerMetadataEndpoint = providerMetadataEndpoint;
		this.requestFactory = requestFactory;
	}
	
	public OIDCProviderMetadata getMetadata(CustomProviderProperties config) throws IOException, ParseException
	{
		if (providerMeta == null || expiresAt < System.currentTimeMillis())
			downloadMetadata(config);
		
		return providerMeta;
	}
	
	private void downloadMetadata(CustomProviderProperties config) throws IOException, ParseException
	{
		HTTPRequest request = wrapRequest(new HTTPRequest(Method.GET, providerMetadataEndpoint), config);
		HTTPResponse response = request.send();
		String cacheControl = response.getCacheControl();
		expiresAt = getExpiresOn(cacheControl);
		String content = response.getContent();
		final String MS_ENDPOINT = "https://login.microsoftonline.com/common/v2.0/.well-known/openid-configuration"; 
		if (MS_ENDPOINT.equals(providerMetadataEndpoint.toExternalForm()))
			content = content.replace("https://login.microsoftonline.com/{tenantid}/v2.0", 
					"https://login.microsoftonline.com/tenantid/v2.0");
		providerMeta = OIDCProviderMetadata.parse(content);
	}
	
	private long getExpiresOn(String cacheControl)
	{
		if (cacheControl == null)
			return System.currentTimeMillis() + DEFAULT_MAX_AGE;
		StringTokenizer stok = new StringTokenizer(cacheControl, " ");
		while (stok.hasMoreTokens())
		{
			String token = stok.nextToken().trim();
			if (token.startsWith("max-age="))
			{
				long validity = Long.parseLong(token.substring("max-age=".length()));
				return System.currentTimeMillis() + validity;
			} else if (token.startsWith("no-cache"))
			{
				return -1;
			}
		}
		return System.currentTimeMillis() + DEFAULT_MAX_AGE;
	}
	
	private HTTPRequest wrapRequest(HTTPRequest httpRequest, CustomProviderProperties config)
	{
		return requestFactory.wrapRequest(httpRequest, config); 
	}
}
