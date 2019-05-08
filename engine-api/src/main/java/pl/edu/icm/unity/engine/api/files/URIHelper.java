/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.api.files;

import java.net.URI;
import java.net.URISyntaxException;

import pl.edu.icm.unity.exceptions.IllegalURIException;

/**
 * URI related methods. 
 * @author P.Piernik
 *
 */
public class URIHelper
{
	public static URI parseURI(String rawURI) throws IllegalURIException
	{
		URI uri;
		try
		{
			uri = new URI(rawURI);
		} catch (URISyntaxException e)
		{
			throw new IllegalURIException("Not supported uri schema");
		}
		validateURI(uri);
		return uri;
	}

	public static void validateURI(URI uri) throws IllegalURIException
	{
		String scheme = uri.getScheme();

		if (scheme == null || scheme.isEmpty() || scheme.equals("file") || scheme.equals("http")
				|| scheme.equals("https") || scheme.equals("data") || scheme.equals("unity.internal"))
		{
			return;
		}
		throw new IllegalURIException("Unknown scheme");
	}

	public static boolean isWebReady(String rawURI)
	{
		URI uri;
		try
		{
			uri = parseURI(rawURI);
		} catch (IllegalURIException e)
		{
			return false;
		}
		return isWebReady(uri);
	}
	
	public static boolean isWebReady(URI uri)
	{
		if (uri != null && uri.getScheme() != null)
		{
			String scheme = uri.getScheme();
			if (scheme.equals("http") || scheme.equals("https") || scheme.equals("data"))
			{
				return true;
			}
		}

		return false;
	}
	
	public static String getPathFromURI(URI uri)
	{
		return uri.isOpaque() ? uri.getSchemeSpecificPart() : uri.getPath();
	}
}
