/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.oidc.metadata;

import java.io.IOException;
import java.net.URL;
import java.text.ParseException;

import org.apache.logging.log4j.Logger;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.oauth2.sdk.http.HTTPRequest;
import com.nimbusds.oauth2.sdk.http.HTTPRequest.Method;
import com.nimbusds.oauth2.sdk.http.HTTPResponse;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.utils.URLFactory;
import pl.edu.icm.unity.oauth.client.HttpRequestConfigurer;


class KeyResource
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_OAUTH, KeyResource.class);

	private final HttpRequestConfigurer requestFactory;

	KeyResource()
	{
		requestFactory = new HttpRequestConfigurer();
	}

	KeyResource(HttpRequestConfigurer requestFactory)
	{

		this.requestFactory = requestFactory;
	}

	public JWKSet getJWKSet(JWKSetRequest jwkSetRequest)
			throws IOException, ParseException
	{
		URL providerMetadataEndpoint = URLFactory.of(jwkSetRequest.url);
		log.debug("Download JWKSet from " + providerMetadataEndpoint);
		HTTPRequest request = requestFactory.secureRequest(new HTTPRequest(Method.GET, providerMetadataEndpoint),
				jwkSetRequest.validator, jwkSetRequest.hostnameChecking);
		HTTPResponse response = request.send();	
		return JWKSet.parse(response.getBody());
	}
}
