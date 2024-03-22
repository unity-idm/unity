/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.api.files;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * A helper class for parsing uri from string and validating it against used
 * schemes
 * 
 * @author P.Piernik
 *
 */
public class URIHelper
{
	public static final Set<String> SUPPORTED_LOCAL_FILE_SCHEMES = new HashSet<>(
			Arrays.asList("file", "unity.internal"));
	
	public static final Set<String> SUPPORTED_URL_SCHEMES = new HashSet<>(
			Arrays.asList("data", "http", "https"));
	
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

		if (scheme == null || scheme.isEmpty() || SUPPORTED_LOCAL_FILE_SCHEMES.contains(scheme) || SUPPORTED_URL_SCHEMES.contains(scheme))
			
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

	public static boolean isLocalFile(String rawURI)
	{
		URI uri;
		try
		{
			uri = parseURI(rawURI);
		} catch (IllegalURIException e)
		{
			return false;
		}
		return isLocalFile(uri);
	}

	public static boolean isWebReady(URI uri)
	{
		if (uri != null && uri.getScheme() != null)
		{
			String scheme = uri.getScheme();
			return SUPPORTED_URL_SCHEMES.contains(scheme);
		}
		if(uri != null && uri.getPath().startsWith("../unitygw/"))
			return true;
		return false;
	}

	public static boolean isLocalFile(URI uri)
	{
		if (uri != null && uri.getScheme() != null)
		{
			String scheme = uri.getScheme();
			return SUPPORTED_LOCAL_FILE_SCHEMES.contains(scheme);
		}
		return false;
	}

	public static String getPathFromURI(URI uri)
	{
		return uri.isOpaque() ? uri.getSchemeSpecificPart() : uri.getPath();
	}
}
