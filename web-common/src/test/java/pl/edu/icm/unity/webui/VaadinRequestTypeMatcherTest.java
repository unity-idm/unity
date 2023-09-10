/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webui;

import org.eclipse.jetty.http.HttpURI;
import org.eclipse.jetty.http.MetaData;
import org.eclipse.jetty.ee8.nested.Request;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.jetty.http.HttpFields.EMPTY;
import static org.eclipse.jetty.http.HttpVersion.HTTP_1_1;

public class VaadinRequestTypeMatcherTest
{

	@Test
	public void shouldNotDetectVaadin8HeartbeatRequest()
	{
		Request request = new Request(null, null);
		request.setMetaData(new MetaData.Request("GET", HttpURI.build("/console"), HTTP_1_1, EMPTY));

		boolean result = VaadinRequestTypeMatcher.isVaadinBackgroundRequest(request);
		assertThat(result).isFalse();
	}

	@Test
	public void shouldDetectVaadin8HeartbeatRequest()
	{
		Request request = new Request(null, null);
		request.setMetaData(new MetaData.Request("GET", HttpURI.build("/HEARTBEAT/"), HTTP_1_1, EMPTY));

		boolean result = VaadinRequestTypeMatcher.isVaadinBackgroundRequest(request);
		assertThat(result).isTrue();
	}

	@Test
	public void shouldDetectVaadin23HeartbeatRequest()
	{
		Request request = new Request(null, null);
		request.setMetaData(new MetaData.Request("POST", HttpURI.build("/?v-r=heartbeat"), HTTP_1_1, EMPTY));

		boolean result = VaadinRequestTypeMatcher.isVaadinBackgroundRequest(request);
		assertThat(result).isTrue();
	}

	@Test
	public void shouldNotDetectVaadin23HeartbeatRequestWhenHttpMethodIsGet()
	{
		Request request = new Request(null, null);
		request.setMetaData(new MetaData.Request("GET", HttpURI.build("/?v-r=heartbeat"), HTTP_1_1, EMPTY));

		boolean result = VaadinRequestTypeMatcher.isVaadinBackgroundRequest(request);
		assertThat(result).isFalse();
	}

	@Test
	public void shouldNotDetectVaadin23HeartbeatRequestWhenQueryParameterIsNotHeartbeat()
	{
		Request request = new Request(null, null);
		request.setMetaData(new MetaData.Request("POST", HttpURI.build("/?v-r=uidl"), HTTP_1_1, EMPTY));

		boolean result = VaadinRequestTypeMatcher.isVaadinBackgroundRequest(request);
		assertThat(result).isFalse();
	}

	@Test
	public void shouldNotDetectVaadin23HeartbeatRequestWhenURLIsNotSlash()
	{
		Request request = new Request(null, null);
		request.setMetaData(new MetaData.Request("POST", HttpURI.build("/ala/?v-r=heartbeat"), HTTP_1_1, EMPTY));

		boolean result = VaadinRequestTypeMatcher.isVaadinBackgroundRequest(request);
		assertThat(result).isFalse();
	}

	@Test
	public void shouldDetectVaadin23PushRequest()
	{
		Request request = new Request(null, null);
		request.setMetaData(new MetaData.Request("GET", HttpURI.build("/?v-r=push"), HTTP_1_1, EMPTY));

		boolean result = VaadinRequestTypeMatcher.isVaadinBackgroundRequest(request);
		assertThat(result).isTrue();
	}

	@Test
	public void shouldNotDetectVaadin23PushRequestWhenHttpMethodIsPost()
	{
		Request request = new Request(null, null);
		request.setMetaData(new MetaData.Request("POST", HttpURI.build("/?v-r=push"), HTTP_1_1, EMPTY));

		boolean result = VaadinRequestTypeMatcher.isVaadinBackgroundRequest(request);
		assertThat(result).isFalse();
	}

	@Test
	public void shouldNotDetectVaadin23PushRequestWhenQueryParameterIsNotHeartbeat()
	{
		Request request = new Request(null, null);
		request.setMetaData(new MetaData.Request("GET", HttpURI.build("/?v-r=uidl"), HTTP_1_1, EMPTY));

		boolean result = VaadinRequestTypeMatcher.isVaadinBackgroundRequest(request);
		assertThat(result).isFalse();
	}

	@Test
	public void shouldNotDetectVaadin23PushRequestWhenURLIsNotSlash()
	{
		Request request = new Request(null, null);
		request.setMetaData(new MetaData.Request("GET", HttpURI.build("/ala/?v-r=push"), HTTP_1_1, EMPTY));

		boolean result = VaadinRequestTypeMatcher.isVaadinBackgroundRequest(request);
		assertThat(result).isFalse();
	}
}