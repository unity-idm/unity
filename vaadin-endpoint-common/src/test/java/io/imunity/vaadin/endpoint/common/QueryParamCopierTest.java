/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.endpoint.common;


import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import pl.edu.icm.unity.engine.api.utils.URLFactory;

public class QueryParamCopierTest
{
	@Test
	void shouldPresaveQueryParamFromDestination() throws MalformedURLException, URISyntaxException
	{
		URI uriWithParams = QueryParamCopier.copyParam("https://destination.com?param1=val1",
				URLFactory.of("https://current.com?param2=val2"));
		Assertions.assertEquals("https://destination.com?param1=val1&param2=val2", uriWithParams.toString());
	}

	@Test
	void shouldSetQueryParamFromCurrentUrl() throws MalformedURLException, URISyntaxException
	{
		URI uriWithParams = QueryParamCopier.copyParam("https://destination.com",
				URLFactory.of("https://current.com?param1=val1"));
		Assertions.assertEquals("https://destination.com?param1=val1", uriWithParams.toString());
	}
}
