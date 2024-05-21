/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.endpoint.common;

import static pl.edu.icm.unity.engine.api.config.UnityServerConfiguration.DEFAULT_WEB_CONTENT_PATH;

import java.io.IOException;

import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;

@Component
class DefaultCssFileLoader
{
	private static final String DEFAULT_STYLE_FILE_NAME = "styles.css";
	private final CssFileLoader cssFileLoader;
	private final String defaultWebContentPath;
	
	DefaultCssFileLoader(UnityServerConfiguration configuration) throws IOException
	{
		defaultWebContentPath = configuration.getValue(DEFAULT_WEB_CONTENT_PATH);
		cssFileLoader = new CssFileLoader(defaultWebContentPath + "/" + DEFAULT_STYLE_FILE_NAME);
	}

	public CssFileLoader get()
	{
		return cssFileLoader;
	}
	
	public String getDefaultWebConentPath()
	{
		return defaultWebContentPath;
	}
}
