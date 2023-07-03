/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.endpoint.common;

import com.vaadin.flow.component.page.Inline.Wrapping;
import com.vaadin.flow.server.AppShellSettings;
import org.apache.logging.log4j.Logger;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class FaviconSetuper
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, FaviconSetuper.class);

	public static void setupFavicon(AppShellSettings settings, UnityServerConfiguration config)
	{
		File webContentsFile = config.getFileValue(UnityServerConfiguration.DEFAULT_WEB_CONTENT_PATH, true);
		Path faviconDefFile = webContentsFile.toPath().resolve(Path.of("VAADIN", "favicon.html"));
		log.debug("Trying to read favicon definitions from {}", faviconDefFile);
		if (Files.isReadable(faviconDefFile))
		{
			String faviconDefContents = readFile(faviconDefFile);
			log.debug("Installing favicon definition from {}", faviconDefFile);
			settings.addInlineWithContents(faviconDefContents, Wrapping.NONE);
		}
	}

	private static String readFile(Path faviconDefFile)
	{
		try
		{
			return Files.readString(faviconDefFile);
		} catch (IOException e)
		{
			throw new IllegalStateException("Unable to read favicon file " + faviconDefFile, e);
		}
	}
}
