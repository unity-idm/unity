/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import com.vaadin.server.ExternalResource;
import com.vaadin.server.FileResource;
import com.vaadin.server.Resource;

public class ImageUtils
{
	/**
	 * Converts URI to Vaadin resource. Supports http(s), data and file schemes.
	 * @param uri
	 * @return
	 * @throws MalformedURLException
	 */
	public static Resource getLogoResource(String uri) throws MalformedURLException
	{
		if (uri == null)
			throw new MalformedURLException("URI is null");
		if (uri.startsWith("http:") || uri.startsWith("https:") || uri.startsWith("data:"))
			return new ExternalResource(uri);
		if (uri.startsWith("file:"))
		{
			URL url = new URL(uri);
			String path = url.getPath();
			return new FileResource(new File(path));
		}
		throw new MalformedURLException("Unsupported logo URI scheme: " + uri);
	}
}
