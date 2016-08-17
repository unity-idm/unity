/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.server;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.engine.api.initializers.ContentInitConf;
import pl.edu.icm.unity.engine.api.initializers.InitializerMode;
import pl.edu.icm.unity.engine.api.initializers.InitializerType;
import pl.edu.icm.unity.exceptions.InternalException;

/**
 * Initialization of database content basing on the
 * {@link UnityServerConfiguration#CONTENT_INITIALIZERS} configuration.
 *
 * @author Roman Krysinski (roman@unity-idm.eu)
 */
@Component
public class ContentInitialization
{
	@Autowired
	private UnityServerConfiguration config;

	public void load()
	{
		Map<InitializerType, List<ContentInitConf>> inizializers = getContentInitializersConfiguration()
				.stream()
				.collect(Collectors.groupingBy(ContentInitConf::getType));

		inizializers.keySet().stream().forEach(type -> {
			switch (type)
			{
			case JSON:
				initializeContentFromJson(inizializers.get(type));
				break;
			case GROOVY:
				initializeContentFromGroovy(inizializers.get(type));
				break;
			default:
				throw new InternalException("Unrecognized initalizer type: " + type.name());
			}
		});
	}

	private void initializeContentFromJson(List<ContentInitConf> list)
	{
		throw new RuntimeException("not yet implemented");
	}

	private void initializeContentFromGroovy(List<ContentInitConf> list)
	{
		throw new RuntimeException("not yet implemented");
	}

	private List<ContentInitConf> getContentInitializersConfiguration()
	{
		Set<String> initializersList = config.getStructuredListKeys(UnityServerConfiguration.CONTENT_INITIALIZERS);
		List<ContentInitConf> inizializers = new ArrayList<>(initializersList.size());
		for (String key: initializersList)
		{
			String file = config.getValue(key + UnityServerConfiguration.CONTENT_INITIALIZERS_FILE);
			String typeStr = config.getValue(key + UnityServerConfiguration.CONTENT_INITIALIZERS_TYPE);
			String modeStr = config.getValue(key + UnityServerConfiguration.CONTENT_INITIALIZERS_MODE);

			InitializerType type = null;
			try
			{
				type = InitializerType.of(typeStr);
			} catch (IllegalArgumentException e)
			{
				throw new InternalException(
						String.format("Invalid initializer type provided for file '%s', was: '%s'. "
								+ "Supported values: ", file, typeStr, InitializerType.typeNamesToString()), e);
			}
			InitializerMode mode = null;
			if (type == InitializerType.JSON)
			{
				try
				{
					mode = InitializerMode.of(modeStr);
				} catch (IllegalArgumentException e)
				{
					throw new InternalException(
							String.format("Invalid initializer mode provided for file '%s', was: '%s'. "
									+ "Supported values: ", file, typeStr, InitializerMode.modesToString()), e);

				}
			}
			inizializers.add(
					ContentInitConf.builder()
						.withFile(file)
						.withType(type)
						.withMode(mode)
						.build());
		}
		return inizializers;
	}
}
