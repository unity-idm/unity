/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.server;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.engine.api.initializers.ContentInitConf;
import pl.edu.icm.unity.engine.api.initializers.InitializerType;
import pl.edu.icm.unity.exceptions.InternalException;

/**
 * Initialization of database content basing on the
 * {@link UnityServerConfiguration#CONTENT_INITIALIZERS} configuration.
 *
 * @author Roman Krysinski (roman@unity-idm.eu)
 */
@Component
public class ContentInitializersExecutor
{
	@Autowired
	private UnityServerConfiguration config;
	@Autowired
	private ContentGroovyExecutor groovyExecutor;

	public void run()
	{
		Map<InitializerType, List<ContentInitConf>> inizializers = getContentInitializersConfiguration().stream()
				.collect(Collectors.groupingBy(ContentInitConf::getType));

		inizializers.forEach((type, configs) ->
		{
			switch (type)
			{
			case GROOVY:
				initializeContentFromGroovy(configs);
				break;
			default:
				throw new InternalException("Unrecognized initalizer type: " + type.name());
			}
		});
	}

	private void initializeContentFromGroovy(List<ContentInitConf> list)
	{
		list.forEach(groovyExecutor::run);
	}

	public List<ContentInitConf> getContentInitializersConfiguration()
	{
		Set<String> initializersList = config.getStructuredListKeys(UnityServerConfiguration.CONTENT_INITIALIZERS);
		List<ContentInitConf> inizializers = new ArrayList<>(initializersList.size());
		for (String key : initializersList)
		{
			File file = config.getFileValue(key + UnityServerConfiguration.CONTENT_INITIALIZERS_FILE, false);
			String typeStr = config.getValue(key + UnityServerConfiguration.CONTENT_INITIALIZERS_TYPE);

			if (!file.exists())
			{
				throw new InternalException(
						String.format("Invalid file provided for initializer: '%s' does not exist.", file.getName()));
			}

			InitializerType type = null;
			try
			{
				type = InitializerType.of(typeStr);
			} catch (IllegalArgumentException e)
			{
				throw new InternalException(String.format(
						"Invalid initializer type provided for file '%s', was: '%s'. Supported values: %s", file,
						typeStr, InitializerType.typeNamesToString()), e);
			}
			inizializers.add(ContentInitConf.builder()
					.withFile(file)
					.withType(type)
					.build());
		}
		return inizializers;
	}
}
