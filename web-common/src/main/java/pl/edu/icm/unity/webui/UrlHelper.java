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
		URI requestURI = Page.getCurrent().getLocation();
		String servletPath = requestURI.getPath();
		String query = requestURI.getQuery() == null ? "" : "?" + requestURI.getQuery();
		String fragment = requestURI.getFragment() == null ? "" : "#" + requestURI.getFragment();
		String currentRelativeURI = servletPath + query + fragment; 
		return currentRelativeURI;
	}
	
	private UrlHelper()
	{
	}
}
