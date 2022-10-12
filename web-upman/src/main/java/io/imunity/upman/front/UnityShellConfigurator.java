/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.upman.front;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.Inline.Wrapping;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.server.AppShellSettings;
import com.vaadin.flow.shared.ui.Transport;
import com.vaadin.flow.theme.Theme;
import com.vaadin.flow.theme.lumo.Lumo;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;

@Push(transport = Transport.LONG_POLLING)
@Theme(themeClass = Lumo.class)
public class UnityShellConfigurator implements AppShellConfigurator
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, UnityShellConfigurator.class);
	private final UnityServerConfiguration config;
	

	@Autowired
	UnityShellConfigurator(UnityServerConfiguration config)
	{
		this.config = config;
	}

	@Override
	public void configurePage(AppShellSettings settings)
	{
		setupFavicon(settings);
	}

	private void setupFavicon(AppShellSettings settings)
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

	private String readFile(Path faviconDefFile)
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
