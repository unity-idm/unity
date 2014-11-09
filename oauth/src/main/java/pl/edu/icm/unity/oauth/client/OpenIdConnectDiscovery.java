/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.client;

import java.io.IOException;
import java.net.URL;
import java.util.StringTokenizer;

import pl.edu.icm.unity.oauth.client.config.CustomProviderProperties;

import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.http.HTTPRequest;
import com.nimbusds.oauth2.sdk.http.HTTPRequest.Method;
import com.nimbusds.oauth2.sdk.http.HTTPResponse;
import com.nimbusds.openid.connect.sdk.op.OIDCProviderMetadata;

/**
 * Retrieves and caches information about OpenID Connect provider.
 *  
 * @author K. Benedyczak
 */
public class OpenIdConnectDiscovery
{
	private URL providerMetadataEndpoint;
	private OIDCProviderMetadata providerMeta;
	private long expiresAt = -1;
	
	
	public OpenIdConnectDiscovery(URL providerMetadataEndpoint)
	{
		this.providerMetadataEndpoint = providerMetadataEndpoint;
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
		StringTokenizer stok = new StringTokenizer(cacheControl, " ");
		while (stok.hasMoreTokens())
		{
			String token = stok.nextToken().trim();
			if (token.startsWith("max-age="))
			{
				long validity = Long.parseLong(token.substring("max-age=".length()));
				expiresAt = System.currentTimeMillis() + validity;
				break;
			} else if (token.startsWith("no-cache"))
			{
				break;
			}
		}
		
		providerMeta = OIDCProviderMetadata.parse(response.getContent());
	}
	
	
	private HTTPRequest wrapRequest(HTTPRequest httpRequest, CustomProviderProperties config)
	{
		return CustomHTTPSRequest.wrapRequest(httpRequest, config); 
	}
}
