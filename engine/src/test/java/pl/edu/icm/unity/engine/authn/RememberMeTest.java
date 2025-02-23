/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.authn;

import static org.assertj.core.api.Assertions.assertThat;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Date;
import java.util.Optional;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;

import pl.edu.icm.unity.base.authn.AuthenticationMethod;
import pl.edu.icm.unity.base.authn.AuthenticationOptionKey;
import pl.edu.icm.unity.base.authn.AuthenticationRealm;
import pl.edu.icm.unity.base.authn.RememberMePolicy;
import pl.edu.icm.unity.base.token.Token;
import pl.edu.icm.unity.engine.DBIntegrationTestBase;
import pl.edu.icm.unity.engine.api.authn.DefaultUnsuccessfulAuthenticationCounter;
import pl.edu.icm.unity.engine.api.authn.LoginSession;
import pl.edu.icm.unity.engine.api.authn.RememberMeProcessor;
import pl.edu.icm.unity.engine.api.authn.RememberMeToken;
import pl.edu.icm.unity.engine.api.authn.RememberMeToken.LoginMachineDetails;
import pl.edu.icm.unity.engine.api.authn.UnsuccessfulAccessCounter;
import pl.edu.icm.unity.engine.api.token.TokensManagement;

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
		LoginMachineDetails loginMachine = new LoginMachineDetails("0.0.0.0", "OS", "Browser");
		rememberMeProcessor.addRememberMeCookieAndUnityToken(response, realm, loginMachine, 1,
				new Date(), 
				new AuthenticationOptionKey("firstFactor", "o1"), 
				new AuthenticationOptionKey("secondFactor", "o2"));
	}
	
	@Test
	public void shouldAddRememberMeCookieAndToken()
	{
		ArgumentCaptor<Cookie> cookieArgument = ArgumentCaptor.forClass(Cookie.class);
		HttpServletResponse response = mock(HttpServletResponse.class);
		
		addCookieAndToken(getRealm(RememberMePolicy.allowForWholeAuthn), response);
		
		verify(response).addCookie(cookieArgument.capture());
		assertThat(cookieArgument.getValue().getValue()).containsSequence("|");
		String[] cookieSplit = cookieArgument.getValue().getValue().split("\\|");
		assertThat(cookieSplit.length).isEqualTo(2);
		String rememberMeSeriesToken = cookieSplit[0];
		
		Token tokenById = tokenMan.getTokenById(RememberMeProcessorImpl.REMEMBER_ME_TOKEN_TYPE,
				rememberMeSeriesToken);
		assertThat(tokenById).isNotNull();
		RememberMeToken rememberMeUnityToken = RememberMeToken
				.getInstanceFromJson(tokenById.getContents());
		assertThat(rememberMeUnityToken.getRememberMePolicy()).isEqualTo(RememberMePolicy.allowForWholeAuthn);
		assertThat(rememberMeUnityToken.getEntity()).isEqualTo(1L);
		assertThat(rememberMeUnityToken.getFirstFactorAuthnOptionId().getAuthenticatorKey()).isEqualTo("firstFactor");
		assertThat(rememberMeUnityToken.getSecondFactorAuthnOptionId().getAuthenticatorKey()).isEqualTo("secondFactor");
		assertThat(rememberMeUnityToken.getMachineDetails().getIp()).isEqualTo("0.0.0.0");
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
		assertThat(cookieArgument.getValue().getValue()).isNotNull();
		assertThat(tokenMan.getAllTokens(RememberMeProcessorImpl.REMEMBER_ME_TOKEN_TYPE)).hasSize(1);
		Cookie added = cookieArgument.getValue();
		when(request.getCookies()).thenReturn(new Cookie[] { added });

		rememberMeProcessor.removeRememberMeWithWholeAuthn(realm.getName(), request,
				response);

		assertThat(tokenMan.getAllTokens(RememberMeProcessorImpl.REMEMBER_ME_TOKEN_TYPE)).hasSize(0);
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
						new DefaultUnsuccessfulAuthenticationCounter(10, 10));
		
		assertThat(loginSession.isPresent()).isEqualTo(true);
		assertThat(loginSession.get().getLogin1stFactorOptionId().getAuthenticatorKey()).isEqualTo("firstFactor");
		assertThat(loginSession.get().getLogin2ndFactorOptionId().getAuthenticatorKey()).isEqualTo("secondFactor");
		assertThat(loginSession.get().getRememberMeInfo().firstFactorSkipped).isEqualTo(true);
		assertThat(loginSession.get().getRememberMeInfo().secondFactorSkipped).isEqualTo(true);
		assertThat(loginSession.get().getAuthenticationMethods()).containsExactly(AuthenticationMethod.u_llc);

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
						new DefaultUnsuccessfulAuthenticationCounter(10, 10));
		
		assertThat(loginSession.isPresent()).isEqualTo(true);
		assertThat(loginSession.get().getLogin1stFactorOptionId().getAuthenticatorKey()).isEqualTo("firstFactor");
		assertThat(loginSession.get().getLogin2ndFactorOptionId().getAuthenticatorKey()).isEqualTo("secondFactor");
		assertThat(loginSession.get().getRememberMeInfo().firstFactorSkipped).isEqualTo(false);
		assertThat(loginSession.get().getRememberMeInfo().secondFactorSkipped).isEqualTo(true);
		assertThat(loginSession.get().getAuthenticationMethods()).containsExactly(AuthenticationMethod.u_llc);
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
						new DefaultUnsuccessfulAuthenticationCounter(10, 10));
		
		assertThat(loginSession.isPresent()).isEqualTo(false);
		
		ArgumentCaptor<Cookie> removedCookieArgument = ArgumentCaptor
				.forClass(Cookie.class);
		verify(response2).addCookie(removedCookieArgument.capture());
		Cookie removedCookie = removedCookieArgument.getValue();
		assertThat(removedCookie.getMaxAge()).isEqualTo(0);

		assertThat(tokenMan.getAllTokens(RememberMeProcessorImpl.REMEMBER_ME_TOKEN_TYPE)).isEmpty();
	}

	@Test
	public void shouldBlockMaliciousRememberAction()
	{
		ArgumentCaptor<Cookie> addedCookieArgument = ArgumentCaptor.forClass(Cookie.class);
		HttpServletResponse response = mock(HttpServletResponse.class);
		UnsuccessfulAccessCounter counter = mock(UnsuccessfulAccessCounter.class);
		
		AuthenticationRealm realm = getRealm(RememberMePolicy.allowForWholeAuthn);
		addCookieAndToken(realm, response);
		verify(response).addCookie(addedCookieArgument.capture());
		Cookie addedCookie = addedCookieArgument.getValue();
		addedCookie.setValue(addedCookie.getValue()+"1");
		
		Optional<LoginSession> loginSession = rememberMeProcessor
				.processRememberedWholeAuthn(setupRequest(addedCookie), response,
						"0.0.0.0", realm,
						counter);	
		
		assertThat(loginSession.isPresent()).isEqualTo(false);
		
		ArgumentCaptor<String> counterArgument = ArgumentCaptor.forClass(String.class);
		verify(counter).unsuccessfulAttempt(counterArgument.capture());
		
		assertThat(counterArgument.getValue()).isEqualTo("0.0.0.0");

		assertThat(tokenMan.getAllTokens(RememberMeProcessorImpl.REMEMBER_ME_TOKEN_TYPE)).isEmpty();
	}
	
}
