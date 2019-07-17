/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webui.authn.endpoints;

import pl.edu.icm.unity.webui.common.FormValidationException;

/**
 * Implementations allow to edit a fixed type endpoint
 * 
 * @author P.Piernik
 *
 */
public interface ServiceEditor
{
	ServiceEditorComponent getEditor(ServiceDefinition endpoint);
	ServiceDefinition getEndpointDefiniton() throws FormValidationException;	
}
