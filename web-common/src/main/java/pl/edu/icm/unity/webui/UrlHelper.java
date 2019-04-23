/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui;

import java.net.URI;

import com.vaadin.server.Page;

/**
 * Utility class to to construct commonly used URLs based on page location.
 *
 * @author Roman Krysinski (roman@unity-idm.eu)
 */
public final class UrlHelper
{

	public static String getCurrentRelativeURI()
	{
		return getRelativeURIFrom(Page.getCurrent().getLocation());
	}
	
	public static String getRelativeURIFrom(URI requestURI)
	{
		String servletPath = requestURI.getRawPath();
		String query = requestURI.getRawQuery() == null ? "" : "?" + requestURI.getRawQuery();
		String fragment = requestURI.getRawFragment() == null ? "" : "#" + requestURI.getRawFragment();
		String currentRelativeURI = servletPath + query + fragment; 
		return currentRelativeURI;
	}
	
	private UrlHelper()
	{
	}
}
