/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.console.views.directory_browser.identities;

import io.imunity.console.views.directory_browser.EntityWithLabel;
import io.imunity.vaadin.endpoint.common.bus.Event;
import pl.edu.icm.unity.base.group.Group;

public record EntityChangedEvent(EntityWithLabel entity, Group group) implements Event
{
}
