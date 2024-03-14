/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.auth.services;

import io.imunity.vaadin.endpoint.common.exceptions.FormValidationException;

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
