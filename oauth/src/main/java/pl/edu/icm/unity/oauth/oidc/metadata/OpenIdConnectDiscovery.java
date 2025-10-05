/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.oidc.metadata;

import java.io.IOException;
import java.net.URL;

import org.apache.logging.log4j.Logger;

import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.http.HTTPRequest;
import com.nimbusds.oauth2.sdk.http.HTTPRequest.Method;
import com.nimbusds.oauth2.sdk.http.HTTPResponse;
import com.nimbusds.openid.connect.sdk.op.OIDCProviderMetadata;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.utils.URLFactory;
import pl.edu.icm.unity.oauth.client.HttpRequestConfigurer;

/**
 * Retrieves information about OpenID Connect provider.
 * 
 * @author K. Benedyczak
 */
class OpenIdConnectDiscovery
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_OAUTH, OpenIdConnectDiscovery.class);

	private final HttpRequestConfigurer requestFactory;

	OpenIdConnectDiscovery()
	{
		requestFactory = new HttpRequestConfigurer();
	}

	OpenIdConnectDiscovery(HttpRequestConfigurer requestFactory)
	{

		this.requestFactory = requestFactory;
	}

	OIDCProviderMetadata getMetadata(OIDCMetadataRequest oidcMetadataRequest)
			throws IOException, ParseException
	{
		URL providerMetadataEndpoint = URLFactory.of(oidcMetadataRequest.url);
		log.debug("Download metadata from " + providerMetadataEndpoint);
		HTTPRequest request = requestFactory.secureRequest(new HTTPRequest(Method.GET, providerMetadataEndpoint),
				oidcMetadataRequest.validator, oidcMetadataRequest.hostnameChecking);
		HTTPResponse response = request.send();
		String content = response.getBody();
		final String MS_ENDPOINT = "https://login.microsoftonline.com/common/v2.0/.well-known/openid-configuration";
		if (MS_ENDPOINT.equals(providerMetadataEndpoint.toExternalForm()))
			content = content.replace("https://login.microsoftonline.com/{tenantid}/v2.0",
					"https://login.microsoftonline.com/tenantid/v2.0");
		return OIDCProviderMetadata.parse(content);
	}

	
}
