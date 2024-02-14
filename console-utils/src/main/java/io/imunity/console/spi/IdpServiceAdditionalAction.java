/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.console.spi;

import com.vaadin.flow.component.Component;

@org.springframework.stereotype.Component
public interface IdpServiceAdditionalAction
{
	String getName();
	String getDisplayedName();
	String getSupportedServiceType();
	ServiceActionRepresentation getActionRepresentation();
	Component getActionContent(String serviceName);
}
