/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.server;

import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.engine.api.initializers.ContentInitConf;
import pl.edu.icm.unity.engine.api.initializers.InitializerType;

/**
 * Executes GROOVY scripts given by user in
 * {@link UnityServerConfiguration#CONTENT_INITIALIZERS} configuration.
 *
 * @author Roman Krysinski (roman@unity-idm.eu)
 */
@Component
public class ContentGroovyExecutor
{
	public void run(ContentInitConf conf)
	{
		if (conf == null || conf.getType() != InitializerType.GROOVY)
			throw new IllegalArgumentException("conf must not be null and must be of " 
					+ InitializerType.GROOVY + " type");
	}
}
