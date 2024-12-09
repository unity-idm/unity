/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.endpoint.common;


import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class QueryParamCopierTest
{
	@Test
	void shouldPresaveQueryParamFromDestination() throws MalformedURLException, URISyntaxException
	{
		URI uriWithParams = QueryParamCopier.copyParam("https://destination.com?param1=val1",
				new URL("https://current.com?param2=val2"));
		Assertions.assertEquals(uriWithParams.toString(), "https://destination.com?param1=val1&param2=val2");
	}

	@Test
	void shouldSetQueryParamFromCurrentUrl() throws MalformedURLException, URISyntaxException
	{
		URI uriWithParams = QueryParamCopier.copyParam("https://destination.com",
				new URL("https://current.com?param1=val1"));
		Assertions.assertEquals(uriWithParams.toString(), "https://destination.com?param1=val1");
	}
}
