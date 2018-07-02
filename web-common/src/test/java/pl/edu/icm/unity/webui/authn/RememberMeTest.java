/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webui.authn;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Date;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;

import pl.edu.icm.unity.base.token.Token;
import pl.edu.icm.unity.engine.DBIntegrationTestBase;
import pl.edu.icm.unity.engine.api.token.TokensManagement;
import pl.edu.icm.unity.types.authn.AuthenticationRealm;
import pl.edu.icm.unity.types.authn.RememberMePolicy;

public class RememberMeTest extends DBIntegrationTestBase
{
	@Autowired
	RememberMeProcessor rememberMeProcessor;

	@Autowired
	TokensManagement tokenMan;

	private AuthenticationRealm getRealm()
	{
		return new AuthenticationRealm("demo", "", 1, 1,
				RememberMePolicy.allowForWholeAuthn, 1, 3);
	}

	@Test
	public void shouldAddRememberMeCookieAndToken()
	{

		ArgumentCaptor<Cookie> cookieArgument = ArgumentCaptor.forClass(Cookie.class);
		HttpServletResponse response = mock(HttpServletResponse.class);

		rememberMeProcessor.addRememberMeCookieAndUnityToken(response, getRealm(),
				"0.0.0.0", 1, new Date(), "firstFactor", "secondFactor");
		verify(response).addCookie(cookieArgument.capture());
		assertThat(cookieArgument.getValue().getValue(), containsString("|"));
		String[] cookieSplit = cookieArgument.getValue().getValue().split("\\|");
		assertThat(cookieSplit.length, is(2));
		String rememberMeSeriesToken = cookieSplit[0];
		Token tokenById = tokenMan.getTokenById(RememberMeProcessor.REMEMBER_ME_TOKEN_TYPE,
				rememberMeSeriesToken);
		assertThat(tokenById, notNullValue());
		RememberMeToken rememberMeUnityToken = RememberMeToken
				.getInstanceFromJson(tokenById.getContents());
		assertThat(rememberMeUnityToken.getRememberMePolicy(),
				is(RememberMePolicy.allowForWholeAuthn));
		assertThat(rememberMeUnityToken.getEntity(), is(1));
		assertThat(rememberMeUnityToken.getFirstFactorAuthnOptionId(), is("firstFactor"));
		assertThat(rememberMeUnityToken.getSecondFactorAuthnOptionId(), is("secondFactor"));
		assertThat(rememberMeUnityToken.getMachineDetails().getIp(), is("0.0.0.0"));
	}

	@Test
	public void shouldRemoveRememberMeCookieAndToken()
	{
		ArgumentCaptor<Cookie> cookieArgument = ArgumentCaptor.forClass(Cookie.class);
		HttpServletResponse response = mock(HttpServletResponse.class);
		HttpServletRequest request = mock(HttpServletRequest.class);
		AuthenticationRealm realm = getRealm();
		rememberMeProcessor.addRememberMeCookieAndUnityToken(response, realm, "", 1,
				new Date(), "", "");
		verify(response).addCookie(cookieArgument.capture());
		assertThat(cookieArgument.getValue().getValue(), notNullValue());
		assertThat(tokenMan.getAllTokens(RememberMeProcessor.REMEMBER_ME_TOKEN_TYPE).size(),
				is(1));
		Cookie added = cookieArgument.getValue();
		when(request.getCookies()).thenReturn(new Cookie[] { added });
		rememberMeProcessor.removeRememberMeWithWholeAuthn(realm.getName(), request,
				response);
		assertThat(tokenMan.getAllTokens(RememberMeProcessor.REMEMBER_ME_TOKEN_TYPE).size(),
				is(0));
	}

}
