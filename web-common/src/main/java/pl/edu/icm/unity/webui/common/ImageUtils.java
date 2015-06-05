/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common;

import java.net.MalformedURLException;
import java.net.URL;

import pl.edu.icm.unity.webui.VaadinEndpointProperties.ScaleMode;

import com.vaadin.server.ExternalResource;
import com.vaadin.server.Resource;
import com.vaadin.server.Sizeable.Unit;
import com.vaadin.server.ThemeResource;
import com.vaadin.ui.Component;

public class ImageUtils
{
	/**
	 * Converts URI to Vaadin resource. Supports http(s), data and file schemes.
	 * In the case of file scheme the URL must be relative and is resolved against the theme directory.
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
			if (path.startsWith("/"))
				throw new MalformedURLException("Image file:// URI must use a "
						+ "relative path to an image in the used theme: " + uri);				
			return new ThemeResource(path);
		}
		throw new MalformedURLException("Unsupported logo URI scheme: " + uri);
	}
	
	public static void setScaleStyling(ScaleMode scaleMode, Component component)
	{
		switch (scaleMode)
		{
		case widthSmall:
			component.addStyleName(Styles.imageWidthSmall.toString());
			break;
		case heightSmall:
			component.addStyleName(Styles.imageHeightSmall.toString());
			break;
		case widthTiny:
			component.addStyleName(Styles.imageWidthTiny.toString());
			break;
		case heightTiny:
			component.addStyleName(Styles.imageHeightTiny.toString());
			break;
		case none:
			component.setHeight(100, Unit.PERCENTAGE);
			break;
		case maxHeightSmall:
			component.addStyleName(Styles.maxHeightSmall.toString());
			break;
		case maxHeightMedium:
			component.addStyleName(Styles.maxHeightMedium.toString());
			break;
		case maxHeightTiny:
			component.addStyleName(Styles.maxHeightTiny.toString());
			break;
		default:
			break;
		}
	}
}
