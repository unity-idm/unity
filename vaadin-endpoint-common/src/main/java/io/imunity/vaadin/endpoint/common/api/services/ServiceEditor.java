/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.endpoint.common.api.services;

import pl.edu.icm.unity.webui.common.FormValidationException;

/**
 * Implementations allow to edit a fixed type service
 * 
 * @author P.Piernik
 *
 */
public interface ServiceEditor
{
	ServiceEditorComponent getEditor(ServiceDefinition endpoint);
	ServiceDefinition getEndpointDefiniton() throws FormValidationException;	
}
