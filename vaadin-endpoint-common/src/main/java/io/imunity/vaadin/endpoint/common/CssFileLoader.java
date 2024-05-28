/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.endpoint.common;

import org.apache.logging.log4j.Logger;
import pl.edu.icm.unity.base.utils.Log;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.concurrent.Executors;

import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

class CssFileLoader
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, CssFileLoader.class);
	private WatchService watchService;
	private File cssFile;

	CssFileLoader(String path)
	{
		try
		{
			watchService = FileSystems.getDefault().newWatchService();
			if (path == null)
				return;
			Path defaultCssFilePath = Paths.get(path);
			cssFile = defaultCssFilePath.toFile();
			WatchKey defaultCssWatchKey = defaultCssFilePath.getParent().register(watchService, ENTRY_MODIFY);
			Executors.newSingleThreadExecutor().execute(() -> reloadFile(defaultCssFilePath, defaultCssWatchKey));
		} catch (IOException e)
		{
			log.error("CSS File has not been loaded, styles will be not working correctly", e);
		}
	}

	private void reloadFile(Path defaultCssFilePath, WatchKey defaultCssWatchKey)
	{
		Path namePath = Paths.get(cssFile.getName());
		while (true)
		{
			WatchKey key;
			try
			{
				key = watchService.take();
			} catch (InterruptedException e)
			{
				throw new RuntimeException(e);
			}

			if (defaultCssWatchKey != key) {
				continue;
			}
			for (WatchEvent<?> event : key.pollEvents())
				if (event.context().equals(namePath))
					cssFile = defaultCssFilePath.toFile();

			defaultCssWatchKey.reset();
		}
	}

	File getCssFile()
	{
		return cssFile;
	}
}
