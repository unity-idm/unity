/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.endpoint.common;

import static java.util.Objects.nonNull;

import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

import org.apache.hc.core5.net.URIBuilder;
import org.apache.logging.log4j.Logger;

import io.imunity.vaadin.endpoint.common.consent_utils.LoginInProgressService;
import pl.edu.icm.unity.base.utils.Log;

public class QueryBuilder
{
	private static final Logger LOG = Log.getLogger(Log.U_SERVER_OAUTH, QueryBuilder.class);

	public static String buildQuery(Map<String, List<String>> urlParameters, String contextKey)
	{
		URIBuilder uriBuilder = new URIBuilder();
		urlParameters.entrySet().stream()
			.forEach(entry -> entry.getValue()
				.forEach(value -> uriBuilder.addParameter(entry.getKey(), value)));

		if (nonNull(contextKey) && !LoginInProgressService.UrlParamSignInContextKey.DEFAULT.getKey().equals(contextKey))
		{
			uriBuilder.addParameter(LoginInProgressService.URL_PARAM_CONTEXT_KEY, contextKey);
		}

		String query = null;
		try
		{
			query = uriBuilder.build().getRawQuery();
		} catch (URISyntaxException e)
		{
			LOG.error("Can't re-encode URL query params, shouldn't happen", e);
		}
		return query == null ? "" : "?" + query;
	}

	public static String buildQuery(Map<String, List<String>> urlParameters)
	{
		return buildQuery(urlParameters, null);
	}
}
