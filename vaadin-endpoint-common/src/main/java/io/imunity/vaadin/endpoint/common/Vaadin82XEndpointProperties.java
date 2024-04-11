/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.endpoint.common;

import eu.unicore.util.configuration.PropertyMD;

import java.io.File;
import java.util.Optional;
import java.util.Properties;

import static java.util.Optional.ofNullable;

public class Vaadin82XEndpointProperties extends VaadinEndpointProperties
{
	public static final String EXTRA_LEFT_PANEL = "extraLeftPanel";

	public static final String EXTRA_RIGHT_PANEL = "extraRightPanel";

	public static final String EXTRA_TOP_PANEL = "extraTopPanel";

	public static final String EXTRA_BOTTOM_PANEL = "extraBottomPanel";

	public static final String CUSTOM_CSS_FILE_NAME = "customCssFileName";

	public static final String SECONDS_BEFORE_SHOWING_SESSION_EXPIRATION_WARNING = "secondsBeforeShowingSessionExpirationWarning";

	public static final String EXTRA_PANELS_AFTER_ATHENTICATION = "addExtraPanelsAfterAuthentication";

	
	public final String defaultWebContentPath;
	public final String defaultCssFileName;

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
		VaadinEndpointProperties.META.put(CUSTOM_CSS_FILE_NAME, new PropertyMD("").
				setDescription("Relative path(starts from web contents path) to an optional CSS file containing custom css file"));
		VaadinEndpointProperties.META.put(SECONDS_BEFORE_SHOWING_SESSION_EXPIRATION_WARNING, new PropertyMD("30").
				setDescription("Seconds before showing session expiration warning notification"));
		VaadinEndpointProperties.META.put(EXTRA_PANELS_AFTER_ATHENTICATION, new PropertyMD("true").
				setDescription("If true, extra panels will also be added to the endpoint's main view"));
	}

	public Vaadin82XEndpointProperties(Properties properties, String defaultWebContentPath, String defaultCssFileName)
	{
		super(properties);
		this.defaultWebContentPath = defaultWebContentPath;
		this.defaultCssFileName = defaultCssFileName;
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

	public Optional<String> getCustomCssFilePath()
	{
		return getPath(CUSTOM_CSS_FILE_NAME);
	}

	private Optional<File> getFile(String key)
	{
		if(getValue(key).isBlank())
			return Optional.empty();
		String value = getWebContentPath() + "/" + getValue(key);
		return Optional.of(new File(value));
	}

	private Optional<String> getPath(String key)
	{
		if(getValue(key).isBlank())
			return Optional.empty();
		String value = getWebContentPath() + "/" + getValue(key);
		return Optional.of(value);
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
