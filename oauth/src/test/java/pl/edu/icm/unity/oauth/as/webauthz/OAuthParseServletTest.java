/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.oauth.as.webauthz;

import com.nimbusds.langtag.LangTag;
import com.nimbusds.langtag.LangTagException;
import com.nimbusds.oauth2.sdk.ResponseType;
import com.nimbusds.oauth2.sdk.Scope;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.oauth2.sdk.id.Identifier;
import com.nimbusds.openid.connect.sdk.AuthenticationRequest;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.internal.verification.Times;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OAuthParseServletTest
{
	@Mock
	private UnityServerConfiguration config;

	@Test
	public void shouldSetFirstSupportedLocaleFromOAuthUIlocales()
			throws IOException, URISyntaxException, LangTagException
	{
		HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
		HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
		OAuthWebRequestValidator validator = Mockito.mock(OAuthWebRequestValidator.class);
		HttpSession session = Mockito.mock(HttpSession.class);
		lenient().when(config.isLocaleSupported(Locale.forLanguageTag("pl-PL"))).thenReturn(true);
		when(request.getSession()).thenReturn(session);
		when(request.getQueryString())
				.thenReturn(new AuthenticationRequest.Builder(new URI("requested"), new ClientID(new Identifier("x")))
						.redirectionURI(new URI("redirect")).scope(Scope.parse("openid"))
						.responseType(ResponseType.CODE)
						.uiLocales(Arrays.asList(LangTag.parse("de-De"), LangTag.parse("pl-PL"))).build()
						.toQueryString());
		OAuthParseServlet servlet = new OAuthParseServlet(null, null, null, validator, config);
		servlet.processRequest(request, response);

		ArgumentCaptor<Cookie> cookieArgument = ArgumentCaptor.forClass(Cookie.class);
		verify(response).addCookie(cookieArgument.capture());
		assertThat(cookieArgument.getValue().getValue()).isEqualTo("pl_PL");
	}

	@Test
	public void shouldSetFirstSupportedLocaleFromOAuthUIlocalesMatchByLangOnly()
			throws IOException, URISyntaxException, LangTagException
	{
		HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
		HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
		OAuthWebRequestValidator validator = Mockito.mock(OAuthWebRequestValidator.class);
		HttpSession session = Mockito.mock(HttpSession.class);
		lenient().when(config.isLocaleSupported(Locale.forLanguageTag("pl"))).thenReturn(true);
		when(request.getSession()).thenReturn(session);
		when(request.getQueryString())
				.thenReturn(new AuthenticationRequest.Builder(new URI("requested"), new ClientID(new Identifier("x")))
						.redirectionURI(new URI("redirect")).scope(Scope.parse("openid"))
						.responseType(ResponseType.CODE)
						.uiLocales(Arrays.asList(LangTag.parse("de-De"), LangTag.parse("pl-PL"))).build()
						.toQueryString());
		OAuthParseServlet servlet = new OAuthParseServlet(null, null, null, validator, config);
		servlet.processRequest(request, response);

		ArgumentCaptor<Cookie> cookieArgument = ArgumentCaptor.forClass(Cookie.class);
		verify(response).addCookie(cookieArgument.capture());
		assertThat(cookieArgument.getValue().getValue()).isEqualTo("pl");
	}

	@Test
	public void shouldSkipInvalidUIlocales() throws IOException, URISyntaxException
	{
		HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
		HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
		OAuthWebRequestValidator validator = Mockito.mock(OAuthWebRequestValidator.class);
		HttpSession session = Mockito.mock(HttpSession.class);
		when(request.getSession()).thenReturn(session);
		when(request.getQueryString())
				.thenReturn(new AuthenticationRequest.Builder(new URI("requested"), new ClientID(new Identifier("x")))
						.redirectionURI(new URI("redirect")).scope(Scope.parse("openid"))
						.responseType(ResponseType.CODE).build().toQueryString() + "&&ui_locales=fr_FR");
		OAuthParseServlet servlet = new OAuthParseServlet(null, null, null, validator, config);
		servlet.processRequest(request, response);
		verify(response, new Times(0)).addCookie(any());
	}
}
