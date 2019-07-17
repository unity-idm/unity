/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webui.authn.endpoints;

import pl.edu.icm.unity.exceptions.EngineException;

/**
 * Factory for {@link ServiceEditor}
 * 
 * @author P.Piernik
 *
 */
public interface ServiceEndpointEditorFactory
{
	String getSupportedEndpointType();

	ServiceEditor createInstance() throws EngineException;
}
