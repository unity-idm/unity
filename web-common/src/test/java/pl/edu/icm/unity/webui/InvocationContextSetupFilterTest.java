/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webui;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Locale;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import pl.edu.icm.unity.engine.api.authn.InvocationContext;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.webui.authn.InvocationContextSetupFilter;
import pl.edu.icm.unity.webui.authn.LanguageCookie;

@ExtendWith(MockitoExtension.class)
public class InvocationContextSetupFilterTest
{
	@Mock
	private UnityServerConfiguration config;

	@Test
	public void shouldSetSupportedLocaleFromCookie() throws IOException, ServletException
	{
		HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
		ServletResponse response = Mockito.mock(ServletResponse.class);
		InvocationContextSetupFilter filter = new InvocationContextSetupFilter(config, null, null, null);
		MockFilterChain chain = new MockFilterChain();
		
		when(config.isLocaleSupported(new Locale("pl-PL"))).thenReturn(true);
		when(request.getCookies()).thenReturn(new Cookie[]
		{ new LanguageCookie("pl-PL") });
		
		filter.doFilter(request, response, chain);
	
		assertThat(chain.getContext().getLocale()).isEqualTo(new Locale("pl-PL"));
	}

	private static class MockFilterChain implements FilterChain
	{
		InvocationContext context;

		@Override
		public void doFilter(ServletRequest request, ServletResponse response) throws IOException, ServletException
		{
			context = InvocationContext.getCurrent();
		}

		public InvocationContext getContext()
		{
			return context;
		}
	}

}
