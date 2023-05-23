/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.translation;

import pl.edu.icm.unity.engine.api.translation.out.AuthenticationFinalizationConfiguration;
import pl.edu.icm.unity.exceptions.EngineException;

public class StopAuthenticationException extends EngineException
{
	public final AuthenticationFinalizationConfiguration finalizationScreenConfiguration;

	public StopAuthenticationException(AuthenticationFinalizationConfiguration finalizationScreenConfiguration)
	{
		this.finalizationScreenConfiguration = finalizationScreenConfiguration;
	}
}
