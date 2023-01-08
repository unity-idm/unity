/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.oauth.oidc.metadata;

import java.io.IOException;
import java.text.ParseException;

import org.junit.Assert;
import org.junit.Test;

import com.nimbusds.jose.jwk.JWKSet;

import eu.unicore.util.httpclient.ServerHostnameCheckingMode;

public class KeyResourceTest
{
	@Test
	public void shouldGetJWKSetFromGoogle() throws IOException, ParseException
	{
		JWKSetRequest request = JWKSetRequest.builder()
				.withHostnameChecking(ServerHostnameCheckingMode.NONE)
				.withUrl("https://www.googleapis.com/oauth2/v3/certs")
				.build();
		KeyResource keyResource = new KeyResource();
		JWKSet jwkSet = keyResource.getJWKSet(request);

		Assert.assertEquals(false, jwkSet.getKeys()
				.isEmpty());

	}
}
