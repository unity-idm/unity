/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import pl.edu.icm.unity.webui.VaadinEndpointProperties.ScaleMode;

import com.vaadin.server.ExternalResource;
import com.vaadin.server.FileResource;
import com.vaadin.server.Resource;
import com.vaadin.server.Sizeable.Unit;
import com.vaadin.ui.Component;

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
	
	public static void setScaleStyling(ScaleMode scaleMode, Component component)
	{
		switch (scaleMode)
		{
		case width100:
			component.addStyleName(Styles.width100.toString());
			break;
		case height100:
			component.addStyleName(Styles.height100.toString());
			component.setHeight(102, Unit.PIXELS);
			break;
		case width50:
			component.addStyleName(Styles.width50.toString());
			break;
		case height50:
			component.addStyleName(Styles.height50.toString());
			component.setHeight(52, Unit.PIXELS);
			break;
		case none:
			component.setHeight(100, Unit.PERCENTAGE);
		case maxHeight100:
			component.addStyleName(Styles.maxHeight100.toString());
			break;
		case maxHeight200:
			component.addStyleName(Styles.maxHeight200.toString());
			break;
		case maxHeight50:
			component.addStyleName(Styles.maxHeight50.toString());
			break;
		default:
			break;
		}
	}
}
