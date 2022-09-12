/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin23.endpoint.common;

import eu.unicore.util.configuration.PropertyMD;
import pl.edu.icm.unity.webui.VaadinEndpointProperties;

import java.io.File;
import java.util.Properties;

public class Vaadin823EndpointProperties extends VaadinEndpointProperties
{
	public static final String EXTRA_LEFT_PANEL = "extraLeftPanel";

	public static final String EXTRA_RIGHT_PANEL = "extraRightPanel";

	public static final String EXTRA_TOP_PANEL = "extraTopPanel";

	public static final String EXTRA_BOTTOM_PANEL = "extraBottomPanel";

	public static final String CUSTOM_CSS = "customCss";

	public static final String SECONDS_BEFORE_SHOWING_SESSION_EXPIRATION_WARNING = "secondsBeforeShowingSessionExpirationWarning";

	static
	{
		VaadinEndpointProperties.META.put(EXTRA_LEFT_PANEL, new PropertyMD("").
				setDescription("Relative path(starts from web contents path) to an optional HTML file containing extra html left panel"));
		VaadinEndpointProperties.META.put(EXTRA_RIGHT_PANEL, new PropertyMD("").
				setDescription("Relative path(starts from web contents path) to an optional HTML file containing extra html right panel"));
		VaadinEndpointProperties.META.put(EXTRA_TOP_PANEL, new PropertyMD("").
				setDescription("Relative path(starts from web contents path) to an optional HTML file containing extra html top panel"));
		VaadinEndpointProperties.META.put(EXTRA_BOTTOM_PANEL, new PropertyMD("").
				setDescription("Relative path(starts from web contents path) to an optional HTML file containing extra html bottom panel"));
		VaadinEndpointProperties.META.put(CUSTOM_CSS, new PropertyMD("").
				setDescription("Relative path(starts from web contents path) to an optional CSS file containing custom css file"));
		VaadinEndpointProperties.META.put(SECONDS_BEFORE_SHOWING_SESSION_EXPIRATION_WARNING, new PropertyMD("30").
				setDescription("Seconds before showing session expiration warning notification"));
	}

	public Vaadin823EndpointProperties(Properties properties)
	{
		super(properties);
	}

	public File getExtraLeftPanel()
	{
		String value = getValue(WEB_CONTENT_PATH) + "/" + getValue(EXTRA_LEFT_PANEL);
		return new File(value);
	}

	public File getExtraRightPanel()
	{
		String value = getValue(WEB_CONTENT_PATH) + "/" + getValue(EXTRA_RIGHT_PANEL);
		return new File(value);
	}

	public File getExtraTopPanel()
	{
		String value = getValue(WEB_CONTENT_PATH) + "/" + getValue(EXTRA_TOP_PANEL);
		return new File(value);
	}

	public File getExtraBottomPanel()
	{
		String value = getValue(WEB_CONTENT_PATH) + "/" + getValue(EXTRA_BOTTOM_PANEL);
		return new File(value);
	}

	public File getCustomCssFile()
	{
		String value = getValue(WEB_CONTENT_PATH) + "/" + getValue(CUSTOM_CSS);
		return new File(value);
	}

	public int getSecondsBeforeShowingSessionExpirationWarning()
	{
		String value = getValue(SECONDS_BEFORE_SHOWING_SESSION_EXPIRATION_WARNING);
		return Integer.parseInt(value);
	}

	public Properties getProperties()
	{
		return properties;
	}

}
