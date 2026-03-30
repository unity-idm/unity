/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.auth;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class NavigationAccessControlInitializerTest
{
	@Test
	void shouldGenerateValidJsForLiteralRedirectWithoutSignInCtx()
	{
		// given
		NavigationAccessControlInitializer initializer =
				NavigationAccessControlInitializer.withAfterSuccessLoginRedirect("/saml-idp/consent");

		// when
		String jsValue = initializer.buildRedirectJsValue(null);

		// then
		assertThat(jsValue).isEqualTo("\"/saml-idp/consent\"");
	}

	@Test
	void shouldGenerateValidJsForLiteralRedirectWithSignInCtx()
	{
		// given
		NavigationAccessControlInitializer initializer =
				NavigationAccessControlInitializer.withAfterSuccessLoginRedirect("/saml-idp/consent");

		// when
		String jsValue = initializer.buildRedirectJsValue("abc-123");

		// then
		assertThat(jsValue).isEqualTo("\"/saml-idp/consent?signInId=abc-123\"");
	}

	@Test
	void shouldGenerateValidJsForDefaultInitializerWithoutSignInCtx()
	{
		// given
		NavigationAccessControlInitializer initializer =
				NavigationAccessControlInitializer.defaultInitializer();

		// when
		String jsValue = initializer.buildRedirectJsValue(null);

		// then
		assertThat(jsValue).isEqualTo("window.location.href");
	}

	@Test
	void shouldGenerateValidJsForDefaultInitializerWithSignInCtx()
	{
		// given
		NavigationAccessControlInitializer initializer =
				NavigationAccessControlInitializer.defaultInitializer();

		// when
		String jsValue = initializer.buildRedirectJsValue("abc-123");

		// then
		assertThat(jsValue).isEqualTo("window.location.href + \"?signInId=abc-123\"");
	}
}
