/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.oauth.as;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import com.nimbusds.jose.JWSAlgorithm;

import eu.unicore.util.configuration.ConfigurationException;

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
	public void shouldThrowConfigExceptionWhenTooShortHMACsecret()
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
		
		TokenSigner signer = new TokenSigner(config, new MockPKIMan());
		assertThat(signer, notNullValue());
		assertThat(signer.getSigningAlgorithm(), is(JWSAlgorithm.HS256));
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
	public void shouldCreateECTokenSigner()
	{
		OAuthASProperties config = OAuthTestUtils.getConfig();
		config.setProperty(OAuthASProperties.SIGNING_ALGORITHM,
				OAuthASProperties.SigningAlgorithms.ES512.toString());
		
		TokenSigner signer = new TokenSigner(config, new MockPKIMan(
					"src/test/resources/demoECKey.p12", "123456"));
		assertThat(signer, notNullValue());
		assertThat(signer.getSigningAlgorithm(), is(JWSAlgorithm.ES512));

	}

	@Test(expected = ConfigurationException.class)
	public void shouldThrowConfigExceptionWhenIncorrectRSAPrivateKey()
	{
		OAuthASProperties config = OAuthTestUtils.getConfig();
		config.setProperty(OAuthASProperties.SIGNING_ALGORITHM,
				OAuthASProperties.SigningAlgorithms.RS512.toString());
		new TokenSigner(config,
				new MockPKIMan("src/test/resources/demoECKey.p12", "123456"));
	}

	@Test
	public void shouldCreateRSTokenSigner()
	{
		OAuthASProperties config = OAuthTestUtils.getConfig();
		config.setProperty(OAuthASProperties.SIGNING_ALGORITHM,
				OAuthASProperties.SigningAlgorithms.RS512.toString());
	
		TokenSigner signer = new TokenSigner(config, new MockPKIMan());
		assertThat(signer, notNullValue());
		assertThat(signer.getSigningAlgorithm(), is(JWSAlgorithm.RS512));

	}

}
