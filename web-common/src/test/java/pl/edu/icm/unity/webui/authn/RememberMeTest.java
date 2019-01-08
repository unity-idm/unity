/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
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

import java.io.IOException;
import java.util.Date;
import java.util.Optional;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;

import pl.edu.icm.unity.base.token.Token;
import pl.edu.icm.unity.engine.DBIntegrationTestBase;
import pl.edu.icm.unity.engine.api.authn.LoginSession;
import pl.edu.icm.unity.engine.api.authn.UnsuccessfulAuthenticationCounter;
import pl.edu.icm.unity.engine.api.token.TokensManagement;
import pl.edu.icm.unity.types.authn.AuthenticationRealm;
import pl.edu.icm.unity.types.authn.RememberMePolicy;

public class RememberMeTest extends DBIntegrationTestBase
{
	@Autowired
	RememberMeProcessor rememberMeProcessor;

	@Autowired
	TokensManagement tokenMan;

	private AuthenticationRealm getRealm(RememberMePolicy policy)
	{
		return new AuthenticationRealm("demo", "", 1, 1,
				policy, 1, 3);
	}

	private HttpServletRequest setupRequest(Cookie cookie)
	{
		HttpServletRequest request = mock(HttpServletRequest.class);
		when(request.getCookies()).thenReturn(new Cookie[] { cookie });
		return request;
	}
	
	private void addCookieAndToken(AuthenticationRealm realm, HttpServletResponse response)
	{
		rememberMeProcessor.addRememberMeCookieAndUnityToken(response, realm, "0.0.0.0", 1,
				new Date(), "firstFactor", "secondFactor");
	}
	
	@Test
	public void shouldAddRememberMeCookieAndToken()
	{
		ArgumentCaptor<Cookie> cookieArgument = ArgumentCaptor.forClass(Cookie.class);
		HttpServletResponse response = mock(HttpServletResponse.class);
		
		addCookieAndToken(getRealm(RememberMePolicy.allowForWholeAuthn), response);
		
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
		assertThat(rememberMeUnityToken.getEntity(), is(1L));
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

		AuthenticationRealm realm = getRealm(RememberMePolicy.allowForWholeAuthn);
		addCookieAndToken(realm, response);

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
	
	@Test
	public void shouldGetRememberedWholeAuthnLoginSession()
	{
		ArgumentCaptor<Cookie> cookieArgument = ArgumentCaptor.forClass(Cookie.class);
		HttpServletResponse response = mock(HttpServletResponse.class);
		AuthenticationRealm realm = getRealm(RememberMePolicy.allowForWholeAuthn);
		
		addCookieAndToken(realm, response);
		verify(response).addCookie(cookieArgument.capture());
		Cookie addedCookie = cookieArgument.getValue();
		
		Optional<LoginSession> loginSession = rememberMeProcessor
				.processRememberedWholeAuthn(setupRequest(addedCookie), response,
						"0.0.0.0", realm,
						new UnsuccessfulAuthenticationCounter(10, 10));
		
		assertThat(loginSession.isPresent(), is(true));
		assertThat(loginSession.get().getLogin1stFactorOptionId(), is("firstFactor"));
		assertThat(loginSession.get().getLogin2ndFactorOptionId(), is("secondFactor"));
		assertThat(loginSession.get().getRememberMeInfo().firstFactorSkipped, is(true));
		assertThat(loginSession.get().getRememberMeInfo().secondFactorSkipped, is(true));
	}

	@Test
	public void shouldGetRememberedSecondFactorLoginSession()
			throws IOException, ServletException
	{
		ArgumentCaptor<Cookie> cookieArgument = ArgumentCaptor.forClass(Cookie.class);
		HttpServletResponse response = mock(HttpServletResponse.class);
		AuthenticationRealm realm = getRealm(RememberMePolicy.allowFor2ndFactor);
		
		addCookieAndToken(realm, response);
		verify(response).addCookie(cookieArgument.capture());
		Cookie addedCookie = cookieArgument.getValue();
		
		Optional<LoginSession> loginSession = rememberMeProcessor
				.processRememberedSecondFactor(setupRequest(addedCookie), response,
						1, "0.0.0.0", realm,
						new UnsuccessfulAuthenticationCounter(10, 10));
		
		assertThat(loginSession.isPresent(), is(true));
		assertThat(loginSession.get().getLogin1stFactorOptionId(), is("firstFactor"));
		assertThat(loginSession.get().getLogin2ndFactorOptionId(), is("secondFactor"));
		assertThat(loginSession.get().getRememberMeInfo().firstFactorSkipped, is(false));
		assertThat(loginSession.get().getRememberMeInfo().secondFactorSkipped, is(true));
	}

	@Test
	public void shouldRemoveCookieIfRealmChange()
	{
		ArgumentCaptor<Cookie> addedCookieArgument = ArgumentCaptor.forClass(Cookie.class);
		HttpServletResponse response = mock(HttpServletResponse.class);
		AuthenticationRealm realm = getRealm(RememberMePolicy.allowForWholeAuthn);
		
		addCookieAndToken(realm, response);
		verify(response).addCookie(addedCookieArgument.capture());
		Cookie addedCookie = addedCookieArgument.getValue();
		
		realm.setRememberMePolicy(RememberMePolicy.allowFor2ndFactor);
		
		HttpServletResponse response2 = mock(HttpServletResponse.class);
		Optional<LoginSession> loginSession = rememberMeProcessor
				.processRememberedSecondFactor(setupRequest(addedCookie), response2,
						1, "0.0.0.0", realm,
						new UnsuccessfulAuthenticationCounter(10, 10));
		
		assertThat(loginSession.isPresent(), is(false));
		
		ArgumentCaptor<Cookie> removedCookieArgument = ArgumentCaptor
				.forClass(Cookie.class);
		verify(response2).addCookie(removedCookieArgument.capture());
		Cookie removedCookie = removedCookieArgument.getValue();
		assertThat(removedCookie.getMaxAge(), is(0));

		assertThat(tokenMan.getAllTokens(RememberMeProcessor.REMEMBER_ME_TOKEN_TYPE).size(),
				is(0));
	}

	@Test
	public void shouldBlockMaliciousRememberAction()
	{
		ArgumentCaptor<Cookie> addedCookieArgument = ArgumentCaptor.forClass(Cookie.class);
		HttpServletResponse response = mock(HttpServletResponse.class);
		UnsuccessfulAuthenticationCounter counter = mock(UnsuccessfulAuthenticationCounter.class);
		
		AuthenticationRealm realm = getRealm(RememberMePolicy.allowForWholeAuthn);
		addCookieAndToken(realm, response);
		verify(response).addCookie(addedCookieArgument.capture());
		Cookie addedCookie = addedCookieArgument.getValue();
		addedCookie.setValue(addedCookie.getValue()+"1");
		
		Optional<LoginSession> loginSession = rememberMeProcessor
				.processRememberedWholeAuthn(setupRequest(addedCookie), response,
						"0.0.0.0", realm,
						counter);	
		
		assertThat(loginSession.isPresent(), is(false));
		
		ArgumentCaptor<String> counterArgument = ArgumentCaptor.forClass(String.class);
		verify(counter).unsuccessfulAttempt(counterArgument.capture());
		
		assertThat(counterArgument.getValue(), is("0.0.0.0"));

		assertThat(tokenMan.getAllTokens(RememberMeProcessor.REMEMBER_ME_TOKEN_TYPE).size(),
				is(0));
	}
	
}
