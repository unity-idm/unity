/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.server;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.engine.api.initializers.ContentInitConf;
import pl.edu.icm.unity.engine.api.initializers.InitializationPhase;
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
	
	public void runPreInitPhase()
	{
		run(InitializationPhase.PRE_INIT);
	}

	public void runPostInitPhase()
	{
		run(InitializationPhase.POST_INIT);
	}
	
	public void run(InitializationPhase phase)
	{
		Map<InitializerType, List<ContentInitConf>> inizializers = getContentInitializersConfiguration()
				.stream()
				.filter(initConf -> initConf.getPhase() == phase)
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
			String fileName = config.getValue(key + UnityServerConfiguration.CONTENT_INITIALIZERS_FILE);
			String typeStr = config.getValue(key + UnityServerConfiguration.CONTENT_INITIALIZERS_TYPE);
			String phaseStr = config.getValue(key + UnityServerConfiguration.CONTENT_INITIALIZERS_PHASE);
			
			File file;
			try
			{
				file = new ClassPathResource(fileName).getFile();
			} catch (IOException e)
			{
				throw new InternalException(String.format("Invalid relative path provided for "
						+ "initializer: '%s'", fileName), e);
			}

			if (!file.exists() || !file.canRead())
			{
				throw new InternalException(String.format("Provided file '%s' does not exist or "
						+ "does not have read permissions.", fileName));
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
			
			InitializationPhase phase = null;
			try
			{
				phase = InitializationPhase.of(phaseStr);
			} catch (IllegalArgumentException e)
			{
				throw new InternalException(String.format(
						"Invalid initializer phase provided for file '%s', was: '%s'. Supported values: %s", file,
						phaseStr, InitializationPhase.typeNamesToString()), e);
			}
			inizializers.add(ContentInitConf.builder()
					.withFile(file)
					.withType(type)
					.withPhase(phase)
					.build());
		}
		return inizializers;
	}
}
