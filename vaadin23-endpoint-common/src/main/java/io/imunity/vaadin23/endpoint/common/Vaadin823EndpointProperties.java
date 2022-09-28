/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin23.endpoint.common;

import eu.unicore.util.configuration.PropertyMD;
import pl.edu.icm.unity.webui.VaadinEndpointProperties;

import java.io.File;
import java.util.Optional;
import java.util.Properties;

import static java.util.Optional.ofNullable;

public class Vaadin823EndpointProperties extends VaadinEndpointProperties
{
	public static final String EXTRA_LEFT_PANEL = "extraLeftPanel";

	public static final String EXTRA_RIGHT_PANEL = "extraRightPanel";

	public static final String EXTRA_TOP_PANEL = "extraTopPanel";

	public static final String EXTRA_BOTTOM_PANEL = "extraBottomPanel";

	public static final String CUSTOM_CSS = "customCss";

	public static final String SECONDS_BEFORE_SHOWING_SESSION_EXPIRATION_WARNING = "secondsBeforeShowingSessionExpirationWarning";

	public final String defaultWebContentPath;

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

	public Vaadin823EndpointProperties(Properties properties, String defaultWebContentPath)
	{
		super(properties);
		this.defaultWebContentPath = defaultWebContentPath;
	}

	public Optional<File> getExtraLeftPanel()
	{
		return getFile(EXTRA_LEFT_PANEL);
	}

	public Optional<File> getExtraRightPanel()
	{
		return getFile(EXTRA_RIGHT_PANEL);
	}

	public Optional<File> getExtraTopPanel()
	{
		return getFile(EXTRA_TOP_PANEL);
	}

	public Optional<File> getExtraBottomPanel()
	{
		return getFile(EXTRA_BOTTOM_PANEL);
	}

	public Optional<File> getCustomCssFile()
	{
		return getFile(CUSTOM_CSS);
	}

	private Optional<File> getFile(String key)
	{
		if(getValue(key).isBlank())
			return Optional.empty();
		String value = getWebContentPath() + "/" + getValue(key);
		return Optional.of(new File(value));
	}

	private String getWebContentPath()
	{
		return ofNullable(getValue(WEB_CONTENT_PATH)).orElse(defaultWebContentPath);
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
