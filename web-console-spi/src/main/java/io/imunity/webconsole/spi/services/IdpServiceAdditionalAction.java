/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole.spi.services;

import com.vaadin.ui.Component;

@org.springframework.stereotype.Component
public interface IdpServiceAdditionalAction
{
	String getName();
	String getDisplayedName();
	String getSupportedServiceType();
	ServiceActionRepresentation getActionRepresentation();
	Component getActionContent(String serviceName);
}
