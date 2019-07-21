/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webui.authn.services;

import pl.edu.icm.unity.exceptions.EngineException;

/**
 * Factory for {@link ServiceEditor}
 * 
 * @author P.Piernik
 *
 */
public interface ServiceEditorFactory
{
	String getSupportedEndpointType();
	ServiceEditor createInstance() throws EngineException;
}
