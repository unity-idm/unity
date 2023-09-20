/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webui;

import static org.assertj.core.api.Assertions.assertThat;

import org.eclipse.jetty.ee8.nested.Request;
import org.eclipse.jetty.http.HttpURI;
import org.eclipse.jetty.http.HttpURI.Mutable;
import org.junit.jupiter.api.Test;

public class VaadinRequestTypeMatcherTest
{

	@Test
	public void shouldNotDetectVaadin8HeartbeatRequest()
	{
		Request request = setupRequest("GET", "/console");

		boolean result = VaadinRequestTypeMatcher.isVaadinBackgroundRequest(request);
		assertThat(result).isFalse();
	}

	@Test
	public void shouldDetectVaadin8HeartbeatRequest()
	{
		Request request = setupRequest("GET", "/HEARTBEAT/");

		boolean result = VaadinRequestTypeMatcher.isVaadinBackgroundRequest(request);
		assertThat(result).isTrue();
	}

	@Test
	public void shouldDetectVaadin23HeartbeatRequest()
	{
		Request request = setupRequest("POST", "/?v-r=heartbeat");

		boolean result = VaadinRequestTypeMatcher.isVaadinBackgroundRequest(request);
		assertThat(result).isTrue();
	}

	@Test
	public void shouldNotDetectVaadin23HeartbeatRequestWhenHttpMethodIsGet()
	{
		Request request = setupRequest("GET", "/?v-r=heartbeat");

		boolean result = VaadinRequestTypeMatcher.isVaadinBackgroundRequest(request);
		assertThat(result).isFalse();
	}

	@Test
	public void shouldNotDetectVaadin23HeartbeatRequestWhenQueryParameterIsNotHeartbeat()
	{
		Request request = setupRequest("POST", "/?v-r=uidl");

		boolean result = VaadinRequestTypeMatcher.isVaadinBackgroundRequest(request);
		assertThat(result).isFalse();
	}

	@Test
	public void shouldNotDetectVaadin23HeartbeatRequestWhenURLIsNotSlash()
	{
		Request request = setupRequest("POST", "/ala/?v-r=heartbeat");

		boolean result = VaadinRequestTypeMatcher.isVaadinBackgroundRequest(request);
		assertThat(result).isFalse();
	}

	@Test
	public void shouldDetectVaadin23PushRequest()
	{
		Request request = setupRequest("GET", "/?v-r=push");

		boolean result = VaadinRequestTypeMatcher.isVaadinBackgroundRequest(request);
		assertThat(result).isTrue();
	}

	@Test
	public void shouldNotDetectVaadin23PushRequestWhenHttpMethodIsPost()
	{
		Request request = setupRequest("POST", "/?v-r=push");

		boolean result = VaadinRequestTypeMatcher.isVaadinBackgroundRequest(request);
		assertThat(result).isFalse();
	}

	@Test
	public void shouldNotDetectVaadin23PushRequestWhenQueryParameterIsNotHeartbeat()
	{
		Request request = setupRequest("GET", "/?v-r=uidl");

		boolean result = VaadinRequestTypeMatcher.isVaadinBackgroundRequest(request);
		assertThat(result).isFalse();
	}

	@Test
	public void shouldNotDetectVaadin23PushRequestWhenURLIsNotSlash()
	{
		Request request = setupRequest("GET", "/ala/?v-r=push");

		boolean result = VaadinRequestTypeMatcher.isVaadinBackgroundRequest(request);
		assertThat(result).isFalse();
	}
	
	private Request setupRequest(String method, String uri)
	{
		Request request = new Request(null, null);
		request.setMethod(method);
		Mutable httpURI = HttpURI.build(uri);
		request.setContext(null, httpURI.getPath());
		request.setHttpURI(httpURI);
		return request;
	}
}