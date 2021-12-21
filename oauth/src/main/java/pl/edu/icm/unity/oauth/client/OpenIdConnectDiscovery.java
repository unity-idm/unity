/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.client;

import java.io.IOException;
import java.net.URL;

import org.apache.logging.log4j.Logger;

import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.http.HTTPRequest;
import com.nimbusds.oauth2.sdk.http.HTTPRequest.Method;
import com.nimbusds.oauth2.sdk.http.HTTPResponse;
import com.nimbusds.openid.connect.sdk.op.OIDCProviderMetadata;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.oauth.client.config.CustomProviderProperties;

/**
 * Retrieves information about OpenID Connect provider.
 * 
 * @author K. Benedyczak
 */
public class OpenIdConnectDiscovery
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_OAUTH, OpenIdConnectDiscovery.class);

	private HttpRequestConfigurer requestFactory;

	public OpenIdConnectDiscovery()
	{
		requestFactory = new HttpRequestConfigurer();
	}

	OpenIdConnectDiscovery(HttpRequestConfigurer requestFactory)
	{

		this.requestFactory = requestFactory;
	}

	public OIDCProviderMetadata getMetadata(String url, CustomProviderProperties config)
			throws IOException, ParseException
	{
		URL providerMetadataEndpoint = new URL(url);
		log.debug("Download metadata from " + providerMetadataEndpoint);
		HTTPRequest request = wrapRequest(new HTTPRequest(Method.GET, providerMetadataEndpoint), config);
		HTTPResponse response = request.send();
		String content = response.getContent();
		final String MS_ENDPOINT = "https://login.microsoftonline.com/common/v2.0/.well-known/openid-configuration";
		if (MS_ENDPOINT.equals(providerMetadataEndpoint.toExternalForm()))
			content = content.replace("https://login.microsoftonline.com/{tenantid}/v2.0",
					"https://login.microsoftonline.com/tenantid/v2.0");
		return OIDCProviderMetadata.parse(content);
	}

	private HTTPRequest wrapRequest(HTTPRequest httpRequest, CustomProviderProperties config)
	{
		return requestFactory.secureRequest(httpRequest, config);
	}
}
