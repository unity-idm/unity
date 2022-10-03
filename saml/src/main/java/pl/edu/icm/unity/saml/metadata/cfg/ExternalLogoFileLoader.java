/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.saml.metadata.cfg;

import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.saml.sp.config.TrustedIdPKey;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Locale;
import java.util.Optional;

import static pl.edu.icm.unity.saml.metadata.cfg.AsyncExternalLogoFileDownloader.*;


@Component
public class ExternalLogoFileLoader
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_SAML, ExternalLogoFileLoader.class);

	private final String workspaceDir;
	private final String defaultLocale;

	ExternalLogoFileLoader(UnityServerConfiguration conf, MessageSource msg)
	{
		workspaceDir = getLogosWorkspace(conf);
		defaultLocale = msg.getLocale().toString();
	}

	public Optional<File> getFile(String federationId, TrustedIdPKey trustedIdPKey, Locale locale)
	{
		String catalogName = federationDirName(federationId);
		String fileName = getLogoFileBasename(trustedIdPKey, locale, defaultLocale);
		String defaultFileName = getLogoFileBasename(trustedIdPKey, defaultLocale);

		try
		{
			Path path = Path.of(workspaceDir, catalogName);
			if(!Files.exists(path))
				return Optional.empty();
			Optional<File> file = findFile(path, fileName + ".*");
			if(file.isPresent())
				return file;
			else
				return findFile(path, defaultFileName + ".*");
		} catch (IOException ioException)
		{
			log.debug("This exception has occurred when logo file has been loaded", ioException);
			return Optional.empty();
		}
	}


	Optional<File> findFile(Path path, String glob) throws IOException
	{
		try (DirectoryStream<Path> paths = Files.newDirectoryStream(path, glob))
		{
			Iterator<Path> iterator = paths.iterator();
			if (iterator.hasNext())
				return Optional.of(iterator.next().toFile());
		}
		return Optional.empty();
	}
}
