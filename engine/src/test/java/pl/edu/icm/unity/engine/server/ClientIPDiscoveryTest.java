/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.server;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.InetSocketAddress;

import org.eclipse.jetty.http.HttpFields;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.server.ConnectionMetaData;
import org.eclipse.jetty.server.Request;
import org.junit.jupiter.api.Test;

public class ClientIPDiscoveryTest
{
	@Test
	public void shouldDiscoverDirectAddress()
	{
		ClientIPDiscovery discovery = new ClientIPDiscovery(0, false);
		Request request = mock(Request.class);
		mockRemoteAddr(request, "10.0.0.1");
		
		String clientIP = discovery.getClientIP(request);
		
		assertThat(clientIP).isEqualTo("10.0.0.1");
	}
	
	@Test
	public void shouldFailWhenXFFMandatoryButMissing()
	{
		ClientIPDiscovery discovery = new ClientIPDiscovery(1, false);
		Request request = mock(Request.class);
		mockRemoteAddr(request, "10.0.0.1");
		mockXFF(request, null);
		
		Throwable throwable = catchThrowable(() -> discovery.getClientIP(request));
		
		assertThat(throwable).isInstanceOf(IllegalArgumentException.class);
	}
	
	@Test
	public void shouldReturnLastXFFIP()
	{
		ClientIPDiscovery discovery = new ClientIPDiscovery(1, false);
		Request request = mock(Request.class);
		mockRemoteAddr(request, "10.0.0.5");
		mockXFF(request, "10.0.0.1, 20.0.0.2, 30.0.0.3");
		
		String clientIP = discovery.getClientIP(request);
		
		assertThat(clientIP).isEqualTo("30.0.0.3");
	}

	@Test
	public void shouldReturnTheOnlyXFFIP()
	{
		ClientIPDiscovery discovery = new ClientIPDiscovery(1, false);
		Request request = mock(Request.class);
		mockRemoteAddr(request, "10.0.0.5");
		mockXFF(request, "10.0.0.1");
		
		String clientIP = discovery.getClientIP(request);
		
		assertThat(clientIP).isEqualTo("10.0.0.1");
	}
	
	@Test
	public void shouldReturnNotLastXFFIP()
	{
		ClientIPDiscovery discovery = new ClientIPDiscovery(2, false);
		Request request = mock(Request.class);
		mockRemoteAddr(request, "10.0.0.5");
		mockXFF(request, "10.0.0.1, 20.0.0.2, 30.0.0.3");
		
		String clientIP = discovery.getClientIP(request);
		
		assertThat(clientIP).isEqualTo("20.0.0.2");
	}

	@Test
	public void shouldFailToReturnWhenXFFTooShort()
	{
		ClientIPDiscovery discovery = new ClientIPDiscovery(2, false);
		Request request = mock(Request.class);
		mockRemoteAddr(request, "10.0.0.5");
		mockXFF(request, "10.0.0.1");
		
		Throwable throwable = catchThrowable(() -> discovery.getClientIP(request));
		
		assertThat(throwable).isInstanceOf(IllegalArgumentException.class);
	}
	
	@Test
	public void shouldFailToReturnInvalidAddress()
	{
		ClientIPDiscovery discovery = new ClientIPDiscovery(1, false);
		Request request = mock(Request.class);
		mockRemoteAddr(request, "10.0.0.5");
		mockXFF(request, "I'm not an address");
		
		Throwable throwable = catchThrowable(() -> discovery.getClientIP(request));
		
		assertThat(throwable).isInstanceOf(IllegalArgumentException.class);
	}
	
	@Test
	public void shouldStripBigPort()
	{
		ClientIPDiscovery discovery = new ClientIPDiscovery(1, false);
		Request request = mock(Request.class);
		mockRemoteAddr(request, "10.0.0.5");
		mockXFF(request, "Foo, 10.0.3.15:56033");
		
		String clientIP = discovery.getClientIP(request);
		
		assertThat(clientIP).isEqualTo("10.0.3.15");
	}

	@Test
	public void shouldStripLowPort()
	{
		ClientIPDiscovery discovery = new ClientIPDiscovery(1, false);
		Request request = mock(Request.class);
		mockRemoteAddr(request, "10.0.0.5");
		mockXFF(request, "Foo, 10.0.3.15:1");
		
		String clientIP = discovery.getClientIP(request);
		
		assertThat(clientIP).isEqualTo("10.0.3.15");
	}

	private void mockRemoteAddr(Request request, String value)
	{
		InetSocketAddress addr = new InetSocketAddress(value, 0);
		ConnectionMetaData connMeta = mock(ConnectionMetaData.class);
		when(connMeta.getRemoteSocketAddress()).thenReturn(addr);
		when(request.getConnectionMetaData()).thenReturn(connMeta);
	}
	
	private void mockXFF(Request request, String value)
	{
		HttpFields headers = mock(HttpFields.class);
		when(headers.get(HttpHeader.X_FORWARDED_FOR)).thenReturn(value);
		when(request.getHeaders()).thenReturn(headers);
	}
	
}
