/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.util.HashSet;

import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.Test;

import com.google.common.collect.Sets;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyType;
import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.openid.connect.sdk.op.OIDCProviderMetadata;

import pl.edu.icm.unity.oauth.as.OAuthASProperties.AccessTokenFormat;
import pl.edu.icm.unity.oauth.as.token.DiscoveryResource;
import pl.edu.icm.unity.oauth.as.token.KeysResource;

public class DiscoveryResourceTest
{
	@Test
	public void testDiscovery() throws ParseException
	{
		OAuthEndpointsCoordinator coordinator = new OAuthEndpointsCoordinator();
		coordinator.registerAuthzEndpoint("https://localhost:233/foo/token", "https://localhost:233/as");
		OAuthASProperties config = OAuthTestUtils.getOIDCConfig();
		DiscoveryResource tested = new DiscoveryResource(config, coordinator, new OAuthScopesService(mock(SystemOAuthScopeProvidersRegistry.class)));
		
		Response resp = tested.getMetadata();
		String body = resp.readEntity(String.class);
		System.out.println(body);
		
		OIDCProviderMetadata parsed = OIDCProviderMetadata.parse(body);
		assertEquals("https://localhost:233/foo/token", parsed.getIssuer().getValue());
		assertEquals("https://localhost:233/as", parsed.getAuthorizationEndpointURI().toString());
		assertEquals("https://localhost:233/foo/token", parsed.getTokenEndpointURI().toString());
		assertEquals("https://localhost:233/foo/userinfo", parsed.getUserInfoEndpointURI().toString());
		assertEquals("https://localhost:233/foo/jwk", parsed.getJWKSetURI().toString());
		assertTrue(Sets.newHashSet("s1", "s2", "openid", "offline_access").equals(new HashSet<>(parsed.getScopes().toStringList())));
		assertEquals(7, parsed.getResponseTypes().size());
	}
	
	@Test
	public void testJWK() throws java.text.ParseException
	{
		OAuthASProperties config = OAuthTestUtils.getOIDCConfig();
		config.setProperty(OAuthASProperties.ACCESS_TOKEN_FORMAT, AccessTokenFormat.JWT.name());
		KeysResource keysResource = new KeysResource(config);
		String keys = keysResource.getKeys();
		JWKSet parsedKeys = JWKSet.parse(keys);
		assertEquals(1, parsedKeys.getKeys().size());
		JWK key = parsedKeys.getKeys().get(0);
		assertEquals(KeyType.RSA, key.getKeyType());
		assertThat(key.getKeyID()).isNotEmpty();
	}

}
