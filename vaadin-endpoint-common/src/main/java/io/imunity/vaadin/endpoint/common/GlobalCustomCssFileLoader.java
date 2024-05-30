/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.endpoint.common;

import org.springframework.stereotype.Component;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static pl.edu.icm.unity.engine.api.config.UnityServerConfiguration.CUSTOM_CSS_FILE_NAME;
import static pl.edu.icm.unity.engine.api.config.UnityServerConfiguration.DEFAULT_WEB_CONTENT_PATH;

@Component
class GlobalCustomCssFileLoader
{
	private final CssFileLoader cssFileLoader;

	GlobalCustomCssFileLoader(UnityServerConfiguration configuration) throws IOException
	{
		String folder = configuration.getValue(DEFAULT_WEB_CONTENT_PATH);
		String filename = configuration.getValue(CUSTOM_CSS_FILE_NAME);
		Path path = filename == null ? null : Paths.get(folder, filename);
		cssFileLoader = new CssFileLoader(path == null ? null : path.toString());
	}

	public CssFileLoader get()
	{
		return cssFileLoader;
	}
}
