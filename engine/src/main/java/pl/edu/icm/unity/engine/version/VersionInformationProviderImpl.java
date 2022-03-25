/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.version;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Properties;

import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.version.VersionInformation;
import pl.edu.icm.unity.engine.api.version.VersionInformationProvider;

@Component
class VersionInformationProviderImpl implements VersionInformationProvider
{
	static final String GIT_PROPERTIES_PATH = "git.properties";

	@Override
	public VersionInformation getVersionInformation()
	{
		Properties gitProperties = new Properties();
		try
		{
			gitProperties.load(getClass().getClassLoader().getResourceAsStream("git.properties"));
		} catch (IOException e)
		{
			throw new RuntimeException("Can not read git properties file", e);
		}

		return VersionInformation.builder().withVersion(gitProperties.getProperty("git.build.version"))
				.withBuildTime(
						LocalDateTime
								.parse(gitProperties.getProperty("git.build.time"),
										DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssZ"))
								.toInstant(ZoneOffset.UTC))
				.build();
	}
}
