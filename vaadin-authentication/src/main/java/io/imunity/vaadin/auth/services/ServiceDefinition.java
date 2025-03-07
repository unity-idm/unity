/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.auth.services;

import pl.edu.icm.unity.base.endpoint.Endpoint.EndpointState;

/**
 * Used by {@link ServiceControllerBaseInt}. Each services can have its own
 * service definition implementation.
 * 
 * @author P.Piernik
 *
 */
public interface ServiceDefinition
{
	String getName();

	EndpointState getState();

	String getType();

	String getBinding();
	
	boolean supportsConfigReloadFromFile();
}
