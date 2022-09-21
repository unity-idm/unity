/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.saml.metadata.cfg;

import org.springframework.stereotype.Component;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.saml.sp.config.TrustedIdPKey;

import java.io.File;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Optional;

import static pl.edu.icm.unity.saml.metadata.cfg.AsyncExternalLogoFileDownloader.federationDirName;
import static pl.edu.icm.unity.saml.metadata.cfg.AsyncExternalLogoFileDownloader.getLogoFileBasename;

@Component
public class ExternalLogoFileLoader
{
	private final String workspaceDir;
	private final String defaultLocale;

	ExternalLogoFileLoader(UnityServerConfiguration conf, MessageSource msg)
	{
		workspaceDir = conf.getValue(UnityServerConfiguration.WORKSPACE_DIRECTORY);
		defaultLocale = msg.getLocale().toString();
	}

	public Optional<File> getFile(String federationId, TrustedIdPKey trustedIdPKey, Locale locale)
	{
		String catalogName = federationDirName(federationId);
		String fileName = getLogoFileBasename(trustedIdPKey, locale, defaultLocale);
		File file = new File(Path.of(workspaceDir, catalogName, fileName).toUri());
		File defaultFile = new File(Path.of(workspaceDir, catalogName, getLogoFileBasename(trustedIdPKey, defaultLocale)).toUri());
		if(file.exists())
			return Optional.of(file);
		else if (defaultFile.exists())
			return Optional.of(defaultFile);

		return Optional.empty();
	}

}
