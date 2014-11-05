/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;

import javax.ws.rs.core.Response;

import org.junit.Test;

import pl.edu.icm.unity.oauth.as.token.DiscoveryResource;
import pl.edu.icm.unity.oauth.as.token.KeysResource;

import com.google.common.collect.Sets;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyType;
import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.openid.connect.sdk.op.OIDCProviderMetadata;

public class DiscoveryResourceTest
{
	@Test
	public void testDiscovery() throws ParseException
	{
		OAuthEndpointsCoordinator coordinator = new OAuthEndpointsCoordinator();
		coordinator.registerAuthzEndpoint("https://localhost:233/foo/token", "https://localhost:233/as");
		OAuthASProperties config = OAuthTestUtils.getConfig();
		DiscoveryResource tested = new DiscoveryResource(config, coordinator);
		
		Response resp = tested.getMetadata();
		String body = resp.readEntity(String.class);
		System.out.println(body);
		
		OIDCProviderMetadata parsed = OIDCProviderMetadata.parse(body);
		assertEquals("https://localhost:233/foo/token", parsed.getIssuer().getValue());
		assertEquals("https://localhost:233/as", parsed.getAuthorizationEndpointURI().toString());
		assertEquals("https://localhost:233/foo/token", parsed.getTokenEndpointURI().toString());
		assertEquals("https://localhost:233/foo/userinfo", parsed.getUserInfoEndpointURI().toString());
		assertEquals("https://localhost:233/foo/jwk", parsed.getJWKSetURI().toString());
		assertTrue(Sets.newHashSet("s1", "s2").equals(new HashSet<>(parsed.getScopes().toStringList())));
		assertEquals(7, parsed.getResponseTypes().size());
	}
	
	public void testJWK() throws java.text.ParseException
	{
		OAuthASProperties config = OAuthTestUtils.getConfig();
		KeysResource keysResource = new KeysResource(config);
		String keys = keysResource.getKeys();
		JWKSet parsedKeys = JWKSet.parse(keys);
		assertEquals(1, parsedKeys.getKeys().size());
		JWK key = parsedKeys.getKeys().get(0);
		assertEquals(KeyType.RSA, key.getKeyType());
	}

}
