/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.server;

import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.initializers.ContentInitConf;
import pl.edu.icm.unity.engine.api.initializers.InitializerType;

/**
 * Loads JSON content from file to database.
 *
 * @author Roman Krysinski (roman@unity-idm.eu)
 */
@Component
public class ContentJsonLoader
{
	public void load(ContentInitConf conf)
	{
		if (conf == null || conf.getType() != InitializerType.JSON)
			throw new IllegalArgumentException("conf must not be null and must be " + InitializerType.JSON + " type");


	}

}
