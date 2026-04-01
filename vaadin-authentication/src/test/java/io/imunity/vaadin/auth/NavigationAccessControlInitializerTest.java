/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.auth;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import io.imunity.vaadin.auth.NavigationAccessControlInitializer.JsExpressionAfterSuccessLoginRedirectProvider;
import io.imunity.vaadin.auth.NavigationAccessControlInitializer.StringAfterSuccessLoginRedirectProvider;

class NavigationAccessControlInitializerTest
{
	@Test
	void shouldGenerateValidJsForLiteralRedirectWithoutSignInCtx()
	{
		// given
		StringAfterSuccessLoginRedirectProvider provider = new StringAfterSuccessLoginRedirectProvider(
			"/saml-idp/consent");

		// when
		String jsValue = provider.get(null);

		// then
		assertThat(jsValue).isEqualTo("\"/saml-idp/consent\"");
	}

	@Test
	void shouldGenerateValidJsForLiteralRedirectWithSignInCtx()
	{
		// given
		StringAfterSuccessLoginRedirectProvider provider = new StringAfterSuccessLoginRedirectProvider(
			"/saml-idp/consent");

		// when
		String jsValue = provider.get("abc-123");

		// then
		assertThat(jsValue).isEqualTo("\"/saml-idp/consent?signInId=abc-123\"");
	}

	@Test
	void shouldGenerateValidJsForDefaultInitializerWithoutSignInCtx()
	{
		// given
		JsExpressionAfterSuccessLoginRedirectProvider provider = new JsExpressionAfterSuccessLoginRedirectProvider();

		// when
		String jsValue = provider.get(null);

		// then
		assertThat(jsValue).isEqualTo("window.location.href");
	}

	@Test
	void shouldGenerateValidJsForDefaultInitializerWithSignInCtx()
	{
		// given
		JsExpressionAfterSuccessLoginRedirectProvider provider = new JsExpressionAfterSuccessLoginRedirectProvider();

		// when
		String jsValue = provider.get("abc-123");

		// then
		assertThat(jsValue).isEqualTo("window.location.href + \"?signInId=abc-123\"");
	}
}
