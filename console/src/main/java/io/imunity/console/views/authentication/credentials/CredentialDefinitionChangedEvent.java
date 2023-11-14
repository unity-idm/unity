/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.console.views.authentication.credentials;


import io.imunity.vaadin.endpoint.common.bus.Event;

public record CredentialDefinitionChangedEvent(
		boolean updatedExisting,
		String name) implements Event
{}
