/*
 * Copyright (c) 2017 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.client;

import javax.net.ssl.SSLSocketFactory;

import com.nimbusds.oauth2.sdk.http.HTTPRequest;

import eu.emi.security.authn.x509.X509CertChainValidator;
import eu.emi.security.authn.x509.impl.SocketFactoryCreator2;
import eu.unicore.util.httpclient.HostnameMismatchCallbackImpl;
import eu.unicore.util.httpclient.ServerHostnameCheckingMode;
import pl.edu.icm.unity.oauth.client.config.CustomProviderProperties;
import pl.edu.icm.unity.oauth.client.config.OAuthClientProperties;

/**
 * Setups TLS handling for nimbus {@link HTTPRequest} aligned with unity configs
 */
public class HttpRequestConfigurer
{
	public static HTTPRequest secureRequest(HTTPRequest request, X509CertChainValidator validator, ServerHostnameCheckingMode mode)
	{
		if (validator != null)
		{
			SSLSocketFactory factory = new SocketFactoryCreator2(validator, new HostnameMismatchCallbackImpl(mode))
					.getSocketFactory();
			request.setSSLSocketFactory(factory);
		}
		return request;
	}
	
	
	public HTTPRequest secureRequest(HTTPRequest httpRequest, OAuthContext context, 
			OAuthClientProperties config)
	{
		CustomProviderProperties providerCfg = config.getProvider(context.getProviderConfigKey());
		return secureRequest(httpRequest, providerCfg);
	}
	
	public HTTPRequest secureRequest(HTTPRequest httpRequest, CustomProviderProperties providerCfg)
	{
		ServerHostnameCheckingMode checkingMode = providerCfg.getEnumValue(
				CustomProviderProperties.CLIENT_HOSTNAME_CHECKING, ServerHostnameCheckingMode.class);
		return secureRequest(httpRequest, providerCfg.getValidator(), checkingMode);
	}
}
