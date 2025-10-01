/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.rp.local.web;

import com.google.common.collect.ImmutableList;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Collections;

public class OAuthRPConfigurationTest
{
	@Test
	public void shouldParseFromStringEmptyScope()
	{
		// given
		LocalOAuthRPConfiguration config = new LocalOAuthRPConfiguration();
		
		// when
		config.fromProperties(propertiesString("unity.oauth2-local-rp.requiredScopes.=\n"));
		
		// then
		Assertions.assertThat(config.getRequiredScopes()).isNotNull().isEmpty();
	}
	
	@Test
	public void shouldParseFromStringBlankScope()
	{
		// given
		LocalOAuthRPConfiguration config = new LocalOAuthRPConfiguration();
		
		// when
		config.fromProperties(propertiesString("unity.oauth2-local-rp.requiredScopes.1=  \n"));
		
		// then
		Assertions.assertThat(config.getRequiredScopes()).isNotNull().isEmpty();
	}
	
	@Test
	public void shouldParseFromStringConfigWithSingleScope()
	{
		// given
		LocalOAuthRPConfiguration config = new LocalOAuthRPConfiguration();
		
		// when
		config.fromProperties(propertiesString("unity.oauth2-local-rp.requiredScopes.1=scope\n"));
		
		// then
		Assertions.assertThat(config.getRequiredScopes()).isNotNull().containsExactly("scope");
	}
	
	@Test
	public void shouldParseFromStringConfigWithMultipleScopes()
	{
		// given
		LocalOAuthRPConfiguration config = new LocalOAuthRPConfiguration();
		
		// when
		config.fromProperties(propertiesString(
				  "unity.oauth2-local-rp.requiredScopes.1=scope\n"
				+ "unity.oauth2-local-rp.requiredScopes.2=scope3\n"));
		
		// then
		Assertions.assertThat(config.getRequiredScopes()).isNotNull().containsExactly("scope", "scope3");
	}
	
	@Test
	public void shouldParseFromStringConfigWithoutScopes()
	{
		// given
		LocalOAuthRPConfiguration config = new LocalOAuthRPConfiguration();
		
		// when
		config.fromProperties(propertiesString(""));
		
		// then
		Assertions.assertThat(config.getRequiredScopes()).isNotNull().isEmpty();
	}
	
	@Test
	public void shouldCreateConfigStringWhenScopesNull()
	{
		// given
		LocalOAuthRPConfiguration config = new LocalOAuthRPConfiguration();
		
		// when
		config.setRequiredScopes(null);
		String configStr = config.toProperties();
		
		// then
		Assertions.assertThat(configStr).doesNotContain("unity.oauth2-local-rp.requiredScopes");
	}
	
	@Test
	public void shouldCreateConfigStringWhenScopesEmpty()
	{
		// given
		LocalOAuthRPConfiguration config = new LocalOAuthRPConfiguration();
		
		// when
		config.setRequiredScopes(Collections.emptyList());
		String configStr = config.toProperties();
		
		// then
		Assertions.assertThat(configStr).doesNotContain("unity.oauth2-local-rp.requiredScopes");
	}
	
	@Test
	public void shouldCreateConfigStringWithSingleScope()
	{
		// given
		LocalOAuthRPConfiguration config = new LocalOAuthRPConfiguration();
		
		// when
		config.setRequiredScopes(ImmutableList.of("scope"));
		String configStr = config.toProperties();
		
		// then
		Assertions.assertThat(configStr).contains("unity.oauth2-local-rp.requiredScopes.1=scope" + System.lineSeparator());
	}
	
	@Test
	public void shouldCreateConfigStringWithMultipleScopes()
	{
		// given
		LocalOAuthRPConfiguration config = new LocalOAuthRPConfiguration();
		
		// when
		config.setRequiredScopes(ImmutableList.of("scope", "\n scope3 "));
		String configStr = config.toProperties();
		
		// then
		Assertions.assertThat(configStr)
			.contains("unity.oauth2-local-rp.requiredScopes.1=scope" + System.lineSeparator())
			.contains("unity.oauth2-local-rp.requiredScopes.2=scope3" + System.lineSeparator());
	}

	
	private String propertiesString(String requiredScopesConfigLine)
	{
		return requiredScopesConfigLine + "\n";
	}
	
	
	
	
	
}
