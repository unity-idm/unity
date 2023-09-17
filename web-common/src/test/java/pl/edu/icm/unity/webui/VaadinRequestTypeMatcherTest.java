/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webui;

import static org.assertj.core.api.Assertions.assertThat;

import org.eclipse.jetty.ee8.nested.Request;
import org.eclipse.jetty.http.HttpURI;
import org.junit.jupiter.api.Test;

public class VaadinRequestTypeMatcherTest
{

	@Test
	public void shouldNotDetectVaadin8HeartbeatRequest()
	{
		Request request = new Request(null, null);
		setRequest(request, "GET", HttpURI.build("/console"));

		boolean result = VaadinRequestTypeMatcher.isVaadinBackgroundRequest(request);
		assertThat(result).isFalse();
	}

	@Test
	public void shouldDetectVaadin8HeartbeatRequest()
	{
		Request request = new Request(null, null);
		setRequest(request, "GET", HttpURI.build("/HEARTBEAT/"));

		boolean result = VaadinRequestTypeMatcher.isVaadinBackgroundRequest(request);
		assertThat(result).isTrue();
	}

	@Test
	public void shouldDetectVaadin23HeartbeatRequest()
	{
		Request request = new Request(null, null);
		setRequest(request, "POST", HttpURI.build("/?v-r=heartbeat"));

		boolean result = VaadinRequestTypeMatcher.isVaadinBackgroundRequest(request);
		assertThat(result).isTrue();
	}

	@Test
	public void shouldNotDetectVaadin23HeartbeatRequestWhenHttpMethodIsGet()
	{
		Request request = new Request(null, null);
		setRequest(request, "GET", HttpURI.build("/?v-r=heartbeat"));

		boolean result = VaadinRequestTypeMatcher.isVaadinBackgroundRequest(request);
		assertThat(result).isFalse();
	}

	@Test
	public void shouldNotDetectVaadin23HeartbeatRequestWhenQueryParameterIsNotHeartbeat()
	{
		Request request = new Request(null, null);
		setRequest(request, "POST", HttpURI.build("/?v-r=uidl"));

		boolean result = VaadinRequestTypeMatcher.isVaadinBackgroundRequest(request);
		assertThat(result).isFalse();
	}

	@Test
	public void shouldNotDetectVaadin23HeartbeatRequestWhenURLIsNotSlash()
	{
		Request request = new Request(null, null);
		setRequest(request, "POST", HttpURI.build("/ala/?v-r=heartbeat"));

		boolean result = VaadinRequestTypeMatcher.isVaadinBackgroundRequest(request);
		assertThat(result).isFalse();
	}

	@Test
	public void shouldDetectVaadin23PushRequest()
	{
		Request request = new Request(null, null);
		setRequest(request, "GET", HttpURI.build("/?v-r=push"));

		boolean result = VaadinRequestTypeMatcher.isVaadinBackgroundRequest(request);
		assertThat(result).isTrue();
	}

	@Test
	public void shouldNotDetectVaadin23PushRequestWhenHttpMethodIsPost()
	{
		Request request = new Request(null, null);
		setRequest(request, "POST", HttpURI.build("/?v-r=push"));

		boolean result = VaadinRequestTypeMatcher.isVaadinBackgroundRequest(request);
		assertThat(result).isFalse();
	}

	@Test
	public void shouldNotDetectVaadin23PushRequestWhenQueryParameterIsNotHeartbeat()
	{
		Request request = new Request(null, null);
		setRequest(request, "GET", HttpURI.build("/?v-r=uidl"));

		boolean result = VaadinRequestTypeMatcher.isVaadinBackgroundRequest(request);
		assertThat(result).isFalse();
	}

	@Test
	public void shouldNotDetectVaadin23PushRequestWhenURLIsNotSlash()
	{
		Request request = new Request(null, null);
		setRequest(request, "GET", HttpURI.build("/ala/?v-r=push"));

		boolean result = VaadinRequestTypeMatcher.isVaadinBackgroundRequest(request);
		assertThat(result).isFalse();
	}
	
	private void setRequest(Request request, String method, HttpURI uri)
	{
		request.setMethod(method);
		request.setHttpURI(uri);
	}
}