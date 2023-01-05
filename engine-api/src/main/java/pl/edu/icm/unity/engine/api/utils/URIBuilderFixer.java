/**********************************************************************
 *                     Copyright (c) 2023, Jirav
 *                        All Rights Reserved
 *
 *         This is unpublished proprietary source code of Jirav.
 *    Reproduction or distribution, in whole or in part, is forbidden
 *          except by express written permission of Jirav, Inc.
 **********************************************************************/
package pl.edu.icm.unity.engine.api.utils;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.http.message.BasicNameValuePair;
import org.apache.hc.core5.net.URIBuilder;

public class URIBuilderFixer
{
	public static URIBuilder newInstance(String uri) throws URISyntaxException
	{
		return newInstance(new URI(uri));
	}
	
	public static URIBuilder newInstance(URI uri)
	{
		URIBuilder builder = new URIBuilder(uri);
		List<NameValuePair> fixedParams = builder.getQueryParams().stream()
				.map(nvp -> new BasicNameValuePair(decodePlusIntoSpace(nvp.getName()), decodePlusIntoSpace(nvp.getValue())))
				.collect(Collectors.toList());
		builder.clearParameters();
		builder.addParameters(fixedParams);
		return builder;
	}
	
	private static String decodePlusIntoSpace(String arg)
	{
		return arg.replace("+", " ");
	}
}
