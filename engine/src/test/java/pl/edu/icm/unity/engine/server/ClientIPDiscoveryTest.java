/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.server;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import javax.servlet.http.HttpServletRequest;

import org.junit.Test;

public class ClientIPDiscoveryTest
{
	@Test
	public void shouldDiscoverDirectAddress()
	{
		ClientIPDiscovery discovery = new ClientIPDiscovery(0, false);
		HttpServletRequest request = mock(HttpServletRequest.class);
		when(request.getRemoteAddr()).thenReturn("10.0.0.1");
		
		String clientIP = discovery.getClientIP(request);
		
		assertThat(clientIP).isEqualTo("10.0.0.1");
	}
	
	@Test
	public void shouldFailWhenXFFMandatoryButMissing()
	{
		ClientIPDiscovery discovery = new ClientIPDiscovery(1, false);
		HttpServletRequest request = mock(HttpServletRequest.class);
		when(request.getRemoteAddr()).thenReturn("10.0.0.1");
		
		Throwable throwable = catchThrowable(() -> discovery.getClientIP(request));
		
		assertThat(throwable).isInstanceOf(IllegalArgumentException.class);
	}
	
	@Test
	public void shouldReturnLastXFFIP()
	{
		ClientIPDiscovery discovery = new ClientIPDiscovery(1, false);
		HttpServletRequest request = mock(HttpServletRequest.class);
		when(request.getRemoteAddr()).thenReturn("10.0.0.5");
		when(request.getHeader("X-Forwarded-For")).thenReturn("10.0.0.1, 20.0.0.2, 30.0.0.3");
		
		String clientIP = discovery.getClientIP(request);
		
		assertThat(clientIP).isEqualTo("30.0.0.3");
	}

	@Test
	public void shouldReturnTheOnlyXFFIP()
	{
		ClientIPDiscovery discovery = new ClientIPDiscovery(1, false);
		HttpServletRequest request = mock(HttpServletRequest.class);
		when(request.getRemoteAddr()).thenReturn("10.0.0.5");
		when(request.getHeader("X-Forwarded-For")).thenReturn("10.0.0.1");
		
		String clientIP = discovery.getClientIP(request);
		
		assertThat(clientIP).isEqualTo("10.0.0.1");
	}
	
	@Test
	public void shouldReturnNotLastXFFIP()
	{
		ClientIPDiscovery discovery = new ClientIPDiscovery(2, false);
		HttpServletRequest request = mock(HttpServletRequest.class);
		when(request.getRemoteAddr()).thenReturn("10.0.0.5");
		when(request.getHeader("X-Forwarded-For")).thenReturn("10.0.0.1, 20.0.0.2, 30.0.0.3");
		
		String clientIP = discovery.getClientIP(request);
		
		assertThat(clientIP).isEqualTo("20.0.0.2");
	}

	@Test
	public void shouldFailToReturnWhenXFFTooShort()
	{
		ClientIPDiscovery discovery = new ClientIPDiscovery(2, false);
		HttpServletRequest request = mock(HttpServletRequest.class);
		when(request.getRemoteAddr()).thenReturn("10.0.0.5");
		when(request.getHeader("X-Forwarded-For")).thenReturn("10.0.0.1");
		
		Throwable throwable = catchThrowable(() -> discovery.getClientIP(request));
		
		assertThat(throwable).isInstanceOf(IllegalArgumentException.class);
	}
	
	@Test
	public void shouldFailToReturnInvalidAddress()
	{
		ClientIPDiscovery discovery = new ClientIPDiscovery(1, false);
		HttpServletRequest request = mock(HttpServletRequest.class);
		when(request.getRemoteAddr()).thenReturn("10.0.0.5");
		when(request.getHeader("X-Forwarded-For")).thenReturn("I'm not an address");
		
		Throwable throwable = catchThrowable(() -> discovery.getClientIP(request));
		
		assertThat(throwable).isInstanceOf(IllegalArgumentException.class);
	}
	
	@Test
	public void shouldStripBigPort()
	{
		ClientIPDiscovery discovery = new ClientIPDiscovery(1, false);
		HttpServletRequest request = mock(HttpServletRequest.class);
		when(request.getRemoteAddr()).thenReturn("10.0.0.5");
		when(request.getHeader("X-Forwarded-For")).thenReturn("Foo, 10.0.3.15:56033");
		
		String clientIP = discovery.getClientIP(request);
		
		assertThat(clientIP).isEqualTo("10.0.3.15");
	}

	@Test
	public void shouldStripLowPort()
	{
		ClientIPDiscovery discovery = new ClientIPDiscovery(1, false);
		HttpServletRequest request = mock(HttpServletRequest.class);
		when(request.getRemoteAddr()).thenReturn("10.0.0.5");
		when(request.getHeader("X-Forwarded-For")).thenReturn("Foo, 10.0.3.15:1");
		
		String clientIP = discovery.getClientIP(request);
		
		assertThat(clientIP).isEqualTo("10.0.3.15");
	}

}
