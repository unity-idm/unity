/*
 * Copyright (c) 2017 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.client;

import com.nimbusds.oauth2.sdk.http.HTTPRequest;

import eu.unicore.util.httpclient.ServerHostnameCheckingMode;
import pl.edu.icm.unity.oauth.client.config.CustomProviderProperties;
import pl.edu.icm.unity.oauth.client.config.OAuthClientProperties;

/**
 * Factory creating {@link HTTPRequest} objects wrapping the given 
 * ones in {@link CustomHTTPSRequest}
 * 
 * @author K. Benedyczak
 */
class CustomHttpRequestFactory
{
	public HTTPRequest wrapRequest(HTTPRequest httpRequest, OAuthContext context, 
			OAuthClientProperties config)
	{
		CustomProviderProperties providerCfg = config.getProvider(context.getProviderConfigKey());
		return wrapRequest(httpRequest, providerCfg);
	}
	
	public HTTPRequest wrapRequest(HTTPRequest httpRequest, CustomProviderProperties providerCfg)
	{
		ServerHostnameCheckingMode checkingMode = providerCfg.getEnumValue(
				CustomProviderProperties.CLIENT_HOSTNAME_CHECKING, ServerHostnameCheckingMode.class);
		
		return new CustomHTTPSRequest(httpRequest, providerCfg.getValidator(), checkingMode); 
	}
}
