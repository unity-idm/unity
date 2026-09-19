/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.endpoint.common;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.net.URIBuilder;

public class QueryParamCopier
{

	public static URI copyParam(String destination, URL source) throws URISyntaxException
	{
		URIBuilder redirect = new URIBuilder(destination);
		URIBuilder current = new URIBuilder(source.toURI());
		Set<String> existingParamNames = redirect.getQueryParams().stream()
				.map(NameValuePair::getName)
				.collect(Collectors.toSet());
		current.getQueryParams().stream()
				.filter(p -> !existingParamNames.contains(p.getName()))
				.forEach(redirect::addParameter);
		return redirect.build();
	}
}
