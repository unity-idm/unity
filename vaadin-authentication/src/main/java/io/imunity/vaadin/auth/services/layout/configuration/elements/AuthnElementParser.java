/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.auth.services.layout.configuration.elements;

import io.imunity.vaadin.endpoint.common.VaadinEndpointProperties;

import java.util.Optional;

public interface AuthnElementParser<T extends AuthnElementConfiguration>
{
	PropertiesRepresentation toProperties(T element);
	
	Optional<T> getConfigurationElement(VaadinEndpointProperties properties, String specEntry);
}
