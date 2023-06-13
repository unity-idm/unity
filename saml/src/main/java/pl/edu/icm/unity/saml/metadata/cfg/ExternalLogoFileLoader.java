/*
 * Copyright (c) 2022 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.saml.metadata.cfg;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Optional;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.saml.sp.config.TrustedIdPKey;


@Component
public class ExternalLogoFileLoader
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_SAML, ExternalLogoFileLoader.class);
	private final String workspaceDir;
	private final String defaultLocale;

	ExternalLogoFileLoader(UnityServerConfiguration conf, MessageSource msg)
	{
		workspaceDir = LogoFilenameUtils.getLogosWorkspace(conf);
		defaultLocale = msg.getLocale().toString();
	}

	public Optional<File> getFile(String federationId, TrustedIdPKey trustedIdPKey, Locale locale)
	{
		String catalogName = LogoFilenameUtils.federationDirName(federationId);
		String fileNameForSpecificLocale = LogoFilenameUtils.getLogoFileBasename(trustedIdPKey, locale, defaultLocale);
		String fileNameWithDefaultLocale = LogoFilenameUtils.getLogoFileBasename(trustedIdPKey, defaultLocale);

		Path path = Path.of(workspaceDir, catalogName);
		if(!Files.exists(path))
			return Optional.empty();

		Optional<File> localizedImage = getFileFromPointer(path, fileNameForSpecificLocale);
		return localizedImage.isPresent() ? localizedImage : getFileFromPointer(path, fileNameWithDefaultLocale);
	}
	
	private Optional<File> getFileFromPointer(Path path, String basename)
	{
		Path pointerFilePath = path.resolve(basename);
		if(!Files.exists(pointerFilePath))
			return Optional.empty();
		try
		{
			String extension = FileUtils.readFileToString(pointerFilePath.toFile(), StandardCharsets.UTF_8);
			return Optional.of(path.resolve(basename + "." + extension).toFile());
		} catch (IOException e)
		{
			log.debug("Can't read cached logo file with extension", e);
			return Optional.empty();
		}
	}
}
