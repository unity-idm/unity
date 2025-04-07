/*
 * Copyright (c) 2025 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.auth.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;

@ExtendWith(MockitoExtension.class)
class ProxyAuthenticationFilterTest
{
	@Mock
	private HttpServletRequest request;

	@Test
	void shouldHandleForwardDispatcherType()
	{
		when(request.getDispatcherType()).thenReturn(DispatcherType.FORWARD);
		when(request.getAttribute(RequestDispatcher.FORWARD_REQUEST_URI)).thenReturn("/test/path");
		when(request.getQueryString()).thenReturn("param1=value1");
		when(request.getParameterMap()).thenReturn(Map.of("param1", new String[]{"value1"}));

		String result = ProxyAuthenticationFilter.getCurrentRelativeURL(request);

		assertEquals("/test/path?param1=value1", result);
	}

	@Test
	void shouldHandleRequestDispatcherType()
	{
		when(request.getDispatcherType()).thenReturn(DispatcherType.REQUEST);
		when(request.getRequestURI()).thenReturn("/context/test/path");
		when(request.getQueryString()).thenReturn("param1=value1");
		when(request.getParameterMap()).thenReturn(Map.of("param1", new String[]{"value1"}));

		String result = ProxyAuthenticationFilter.getCurrentRelativeURL(request);

		assertEquals("/context/test/path?param1=value1", result);
	}

	@Test
	void shouldHandleNullQueryString()
	{
		when(request.getDispatcherType()).thenReturn(DispatcherType.REQUEST);
		when(request.getRequestURI()).thenReturn("/context/test/path");
		when(request.getQueryString()).thenReturn(null);

		String result = ProxyAuthenticationFilter.getCurrentRelativeURL(request);

		assertEquals("/context/test/path", result);
	}

	@Test
	void shouldThrowExceptionForUnsupportedDispatcherType()
	{
		when(request.getDispatcherType()).thenReturn(DispatcherType.INCLUDE);

		assertThrows(IllegalStateException.class,
			() -> ProxyAuthenticationFilter.getCurrentRelativeURL(request));
	}

	@Test
	void shouldHandleNullForwardRequestUri()
	{
		when(request.getDispatcherType()).thenReturn(DispatcherType.FORWARD);
		when(request.getAttribute(RequestDispatcher.FORWARD_REQUEST_URI)).thenReturn(null);
		when(request.getQueryString()).thenReturn(null);

		String result = ProxyAuthenticationFilter.getCurrentRelativeURL(request);

		assertEquals("/", result);
	}
}
