/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webui.console.services.authnlayout.configuration.elements;

import java.util.Optional;

import pl.edu.icm.unity.webui.VaadinEndpointProperties;

public interface AuthnElementParser<T extends AuthnElementConfiguration>
{
	PropertiesRepresentation toProperties(T element);
	
	Optional<T> getConfigurationElement(VaadinEndpointProperties properties, String specEntry);
}
