/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.console.views.directory_browser.identities.credentials;


import io.imunity.vaadin.endpoint.common.bus.Event;


public record CredentialDefinitionChangedEvent(boolean updatedExisting, String name) implements Event
{
}
