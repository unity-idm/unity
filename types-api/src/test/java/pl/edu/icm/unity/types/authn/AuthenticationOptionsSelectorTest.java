/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types.authn;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.junit.Test;

public class AuthenticationOptionsSelectorTest
{
	@Test
	public void shouldMatchSpecificOption()
	{
		AuthenticationOptionsSelector selector = new AuthenticationOptionsSelector("authenticator1", "option1", Optional.empty());
		AuthenticationOptionKey authnOption = new AuthenticationOptionKey("authenticator1", "option1");
		
		assertThat(selector.matchesAuthnOption(authnOption)).isTrue();
	}

	@Test
	public void shouldMatchWildcardOption()
	{
		AuthenticationOptionsSelector selector = AuthenticationOptionsSelector.allForAuthenticator("authenticator1");
		AuthenticationOptionKey authnOption = new AuthenticationOptionKey("authenticator1", "option1");
		
		assertThat(selector.matchesAuthnOption(authnOption)).isTrue();		
	}

	@Test
	public void shouldNotMatchSpecificOptionDifferingInAuthenticator()
	{
		AuthenticationOptionsSelector selector = new AuthenticationOptionsSelector("authenticator1", "option1", Optional.empty());
		AuthenticationOptionKey authnOption = new AuthenticationOptionKey("authenticator2", "option1");
		
		assertThat(selector.matchesAuthnOption(authnOption)).isFalse();
	}

	@Test
	public void shouldNotMatchSpecificOptionDifferingInOption()
	{
		AuthenticationOptionsSelector selector = new AuthenticationOptionsSelector("authenticator1", "option1", Optional.empty());
		AuthenticationOptionKey authnOption = new AuthenticationOptionKey("authenticator1", "option2");
		
		assertThat(selector.matchesAuthnOption(authnOption)).isFalse();
	}

	@Test
	public void shouldNotMatchWildcardOption()
	{
		AuthenticationOptionsSelector selector = AuthenticationOptionsSelector.allForAuthenticator("authenticator1");
		AuthenticationOptionKey authnOption = new AuthenticationOptionKey("authenticator2", "option1");
		
		assertThat(selector.matchesAuthnOption(authnOption)).isFalse();
	}

}
