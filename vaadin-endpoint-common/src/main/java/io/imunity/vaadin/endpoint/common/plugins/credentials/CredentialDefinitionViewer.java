/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.endpoint.common.plugins.credentials;

import com.vaadin.flow.component.Component;

public interface CredentialDefinitionViewer
{
	Component getViewer(String credentialDefinitionConfiguration);
}
