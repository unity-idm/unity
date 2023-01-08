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

/**
 * Setups TLS handling for nimbus {@link HTTPRequest} aligned with unity configs
 */
public class HttpRequestConfigurer
{
	public  HTTPRequest secureRequest(HTTPRequest request, X509CertChainValidator validator, ServerHostnameCheckingMode mode)
	{
		if (validator != null)
		{
			SSLSocketFactory factory = new SocketFactoryCreator2(validator, new HostnameMismatchCallbackImpl(mode))
					.getSocketFactory();
			request.setSSLSocketFactory(factory);
		}
		return request;
	}
}
