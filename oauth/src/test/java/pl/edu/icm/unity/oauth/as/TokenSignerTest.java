/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.oauth.as;

import static org.junit.Assert.fail;

import org.junit.Test;

import eu.unicore.util.configuration.ConfigurationException;
import pl.edu.icm.unity.oauth.as.OAuthASProperties;

public class TokenSignerTest
{
	
	@Test(expected = ConfigurationException.class)
	public void shouldThrowConfigExceptionWhenEmptyHMACsecret()
	{
		OAuthASProperties config = OAuthTestUtils.getConfig();
		config.setProperty(OAuthASProperties.SIGNING_ALGORITHM,
				OAuthASProperties.SigningAlgorithms.HS512.toString());
		new TokenSigner(config, new MockPKIMan());
	}
	
	@Test(expected = ConfigurationException.class)
	public void shouldThrowConfigExceptionWhenToShortHMACsecret()
	{
		OAuthASProperties config = OAuthTestUtils.getConfig();
		config.setProperty(OAuthASProperties.SIGNING_SECRET,
				"b8e7ae12510bdfb1812e463a7f086122cf37e4f7");
		config.setProperty(OAuthASProperties.SIGNING_ALGORITHM,
				OAuthASProperties.SigningAlgorithms.HS512.toString());
		new TokenSigner(config, new MockPKIMan());
	}

	@Test
	public void shouldCreateHMACTokenSigner()
	{
		OAuthASProperties config = OAuthTestUtils.getConfig();
		config.setProperty(OAuthASProperties.SIGNING_SECRET,
				"b8e7ae12510bdfb1812e463a7f086122cf37e4f7");
		config.setProperty(OAuthASProperties.SIGNING_ALGORITHM,
				OAuthASProperties.SigningAlgorithms.HS256.toString());
		try
		{
			new TokenSigner(config, new MockPKIMan());

		} catch (Exception e)
		{
			fail("Cannot create HMAC token signer");
		}
	}

	@Test(expected = ConfigurationException.class)
	public void shouldThrowConfigExceptionWhenIncorrectECPrivateKey()
	{
		OAuthASProperties config = OAuthTestUtils.getConfig();
		config.setProperty(OAuthASProperties.SIGNING_ALGORITHM,
				OAuthASProperties.SigningAlgorithms.ES512.toString());
		new TokenSigner(config, new MockPKIMan());
	}
	
	@Test
	public void shouldCreateRSTokenSigner()
	{
		OAuthASProperties config = OAuthTestUtils.getConfig();
		config.setProperty(OAuthASProperties.SIGNING_ALGORITHM,
				OAuthASProperties.SigningAlgorithms.RS512.toString());
		try
		{
			new TokenSigner(config, new MockPKIMan());

		} catch (Exception e)
		{
			fail("Cannot create RS token signer");
		}
	}
}
