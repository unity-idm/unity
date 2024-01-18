/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.console.views.directory_browser.group_browser;

import io.imunity.vaadin.endpoint.common.bus.Event;
import pl.edu.icm.unity.base.group.Group;

/**
 * Event for directory browser panel to inform, which group is chosen
 * If group is null and multi flag is true it means that more then one group is selected
 * If group is null and multi flag is false it means that no group is selected
 */
public record GroupChangedEvent(
		Group group,
		boolean multi) implements Event
{

}
