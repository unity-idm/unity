/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.endpoint.common;

import org.springframework.stereotype.Component;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;

import java.io.IOException;

import static pl.edu.icm.unity.engine.api.config.UnityServerConfiguration.DEFAULT_CSS_FILE_NAME;
import static pl.edu.icm.unity.engine.api.config.UnityServerConfiguration.DEFAULT_WEB_CONTENT_PATH;

@Component
class DefaultCssFileLoader
{
	private final CssFileLoader cssFileLoader;

	DefaultCssFileLoader(UnityServerConfiguration configuration) throws IOException
	{
		String folder = configuration.getValue(DEFAULT_WEB_CONTENT_PATH);
		String filename = configuration.getValue(DEFAULT_CSS_FILE_NAME);
		cssFileLoader = new CssFileLoader(folder + "/" + filename);
	}

	public CssFileLoader get()
	{
		return cssFileLoader;
	}
}