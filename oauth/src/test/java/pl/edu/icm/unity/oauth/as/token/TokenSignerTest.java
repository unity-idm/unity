/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.oauth.as.token;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.nimbusds.jose.JWSAlgorithm;

import eu.unicore.util.configuration.ConfigurationException;
import pl.edu.icm.unity.oauth.as.MockPKIMan;
import pl.edu.icm.unity.oauth.as.OAuthASProperties;
import pl.edu.icm.unity.oauth.as.OAuthTestUtils;
import pl.edu.icm.unity.oauth.as.TokenSigner;

public class TokenSignerTest
{

	@Test
	public void shouldThrowConfigExceptionWhenEmptyHMACsecret()
	{
		OAuthASProperties config = OAuthTestUtils.getOIDCConfig();
		config.setProperty(OAuthASProperties.SIGNING_ALGORITHM,
				OAuthASProperties.SigningAlgorithms.HS512.toString());
		Assertions.assertThrows(ConfigurationException.class, () -> new TokenSigner(config, new MockPKIMan()));
	}

	@Test
	public void shouldThrowConfigExceptionWhenTooShortHMACsecret()
	{
		OAuthASProperties config = OAuthTestUtils.getOIDCConfig();
		config.setProperty(OAuthASProperties.SIGNING_SECRET,
				"b8e7ae12510bdfb1812e463a7f086122cf37e4f7");
		config.setProperty(OAuthASProperties.SIGNING_ALGORITHM,
				OAuthASProperties.SigningAlgorithms.HS512.toString());
		Assertions.assertThrows(ConfigurationException.class, () -> new TokenSigner(config, new MockPKIMan()));
	}

	@Test
	public void shouldCreateHMACTokenSigner()
	{
		OAuthASProperties config = OAuthTestUtils.getOIDCConfig();
		config.setProperty(OAuthASProperties.SIGNING_SECRET,
				"b8e7ae12510bdfb1812e463a7f086122cf37e4f7");
		config.setProperty(OAuthASProperties.SIGNING_ALGORITHM,
				OAuthASProperties.SigningAlgorithms.HS256.toString());	
		
		TokenSigner signer = new TokenSigner(config, new MockPKIMan());
		assertThat(signer).isNotNull();
		assertThat(signer.getSigningAlgorithm()).isEqualTo(JWSAlgorithm.HS256);
	}

	@Test
	public void shouldThrowConfigExceptionWhenIncorrectECPrivateKey()
	{
		OAuthASProperties config = OAuthTestUtils.getOIDCConfig();
		config.setProperty(OAuthASProperties.SIGNING_ALGORITHM,
				OAuthASProperties.SigningAlgorithms.ES512.toString());
		Assertions.assertThrows(ConfigurationException.class, () -> new TokenSigner(config, new MockPKIMan()));
	}
	
	@Test
	public void shouldCreateECTokenSigner()
	{
		OAuthASProperties config = OAuthTestUtils.getOIDCConfig();
		config.setProperty(OAuthASProperties.SIGNING_ALGORITHM,
				OAuthASProperties.SigningAlgorithms.ES512.toString());
		
		TokenSigner signer = new TokenSigner(config, new MockPKIMan(
					"src/test/resources/demoECKey.p12", "123456"));
		assertThat(signer).isNotNull();
		assertThat(signer.getSigningAlgorithm()).isEqualTo(JWSAlgorithm.ES512);

	}

	@Test
	public void shouldThrowConfigExceptionWhenIncorrectRSAPrivateKey()
	{
		OAuthASProperties config = OAuthTestUtils.getOIDCConfig();
		config.setProperty(OAuthASProperties.SIGNING_ALGORITHM, OAuthASProperties.SigningAlgorithms.RS512.toString());

		Assertions.assertThrows(ConfigurationException.class,
				() -> new TokenSigner(config, new MockPKIMan("src/test/resources/demoECKey.p12", "123456")));

	}

	@Test
	public void shouldCreateRSTokenSigner()
	{
		OAuthASProperties config = OAuthTestUtils.getOIDCConfig();
		config.setProperty(OAuthASProperties.SIGNING_ALGORITHM,
				OAuthASProperties.SigningAlgorithms.RS512.toString());
	
		TokenSigner signer = new TokenSigner(config, new MockPKIMan());
		assertThat(signer).isNotNull();
		assertThat(signer.getSigningAlgorithm()).isEqualTo(JWSAlgorithm.RS512);

	}

}
