/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.rp.web;

import java.util.Collections;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import com.google.common.collect.ImmutableList;

import io.codearte.catchexception.shade.mockito.Mockito;
import pl.edu.icm.unity.engine.api.PKIManagement;
import pl.edu.icm.unity.oauth.as.token.OAuthAccessTokenRepository;

public class OAuthRPConfigurationTest
{
	@Test
	public void shouldParseFromStringEmptyScope()
	{
		// given
		OAuthRPConfiguration config = mockConfig();
		
		// when
		config.fromProperties(propertiesString("unity.oauth2-rp.requiredScopes.=\n"));
		
		// then
		Assertions.assertThat(config.getRequiredScopes()).isNotNull().isEmpty();
	}
	
	@Test
	public void shouldParseFromStringBlankScope()
	{
		// given
		OAuthRPConfiguration config = mockConfig();
		
		// when
		config.fromProperties(propertiesString("unity.oauth2-rp.requiredScopes.1=  \n"));
		
		// then
		Assertions.assertThat(config.getRequiredScopes()).isNotNull().isEmpty();
	}
	
	@Test
	public void shouldParseFromStringConfigWithSingleScope()
	{
		// given
		OAuthRPConfiguration config = mockConfig();
		
		// when
		config.fromProperties(propertiesString("unity.oauth2-rp.requiredScopes.1=scope\n"));
		
		// then
		Assertions.assertThat(config.getRequiredScopes()).isNotNull().containsExactly("scope");
	}
	
	@Test
	public void shouldParseFromStringConfigWithMultipleScopes()
	{
		// given
		OAuthRPConfiguration config = mockConfig();
		
		// when
		config.fromProperties(propertiesString(
				  "unity.oauth2-rp.requiredScopes.1=scope\n"
				+ "unity.oauth2-rp.requiredScopes.2=scope3\n"));
		
		// then
		Assertions.assertThat(config.getRequiredScopes()).isNotNull().containsExactly("scope", "scope3");
	}
	
	@Test
	public void shouldParseFromStringConfigWithoutScopes()
	{
		// given
		OAuthRPConfiguration config = mockConfig();
		
		// when
		config.fromProperties(propertiesString(""));
		
		// then
		Assertions.assertThat(config.getRequiredScopes()).isNotNull().isEmpty();
	}
	
	@Test
	public void shouldCreateConfigStringWhenScopesNull()
	{
		// given
		OAuthRPConfiguration config = mockWithRequiredConfig();
		
		// when
		config.setRequiredScopes(null);
		String configStr = config.toProperties();
		
		// then
		Assertions.assertThat(configStr).doesNotContain("unity.oauth2-rp.requiredScopes");
	}
	
	@Test
	public void shouldCreateConfigStringWhenScopesEmpty()
	{
		// given
		OAuthRPConfiguration config = mockWithRequiredConfig();
		
		// when
		config.setRequiredScopes(Collections.emptyList());
		String configStr = config.toProperties();
		
		// then
		Assertions.assertThat(configStr).doesNotContain("unity.oauth2-rp.requiredScopes");
	}
	
	@Test
	public void shouldCreateConfigStringWithSingleScope()
	{
		// given
		OAuthRPConfiguration config = mockWithRequiredConfig();
		
		// when
		config.setRequiredScopes(ImmutableList.of("scope"));
		String configStr = config.toProperties();
		
		// then
		Assertions.assertThat(configStr).contains("unity.oauth2-rp.requiredScopes.1=scope\n");
	}
	
	@Test
	public void shouldCreateConfigStringWithMultipleScopes()
	{
		// given
		OAuthRPConfiguration config = mockWithRequiredConfig();
		
		// when
		config.setRequiredScopes(ImmutableList.of("scope", "\n scope3 "));
		String configStr = config.toProperties();
		
		// then
		Assertions.assertThat(configStr)
			.contains("unity.oauth2-rp.requiredScopes.1=scope\n")
			.contains("unity.oauth2-rp.requiredScopes.2=scope3\n");
	}

	
	private String propertiesString(String requiredScopesConfigLine)
	{
		return requiredScopesConfigLine+ "\n"
				+ "unity.oauth2-rp.cacheTime=2\n"
				+ "unity.oauth2-rp.translationProfile=tr-oauth\n"
				+ "unity.oauth2-rp.verificationProtocol=internal\n"
				+ "unity.oauth2-rp.clientSecret";
	}
	
	
	private OAuthRPConfiguration mockConfig()
	{
		PKIManagement pkiMan = Mockito.mock(PKIManagement.class);
		OAuthAccessTokenRepository tokensDAO = Mockito.mock(OAuthAccessTokenRepository.class);
		return new OAuthRPConfiguration(pkiMan, tokensDAO);
	}
	
	private OAuthRPConfiguration mockWithRequiredConfig()
	{
		OAuthRPConfiguration config = mockConfig();
		config.setClientId("clientId");
		config.setClientSecret("clientSecret");
		config.setVerificationEndpoint("/verificationEntpoind");
		return config;
	}
}
