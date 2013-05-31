/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.mock;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.endpoint.BindingAuthn;

public interface MockBinding extends BindingAuthn 
{
	public Long authenticate() throws EngineException;
}
