/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.webconsole.directoryBrowser.identities;

import pl.edu.icm.unity.base.group.Group;
import pl.edu.icm.unity.webui.bus.Event;
import pl.edu.icm.unity.webui.common.EntityWithLabel;

public class EntityChangedEvent implements Event
{
	private EntityWithLabel entity;
	private Group group;

	public EntityChangedEvent(EntityWithLabel entity, Group group)
	{
		this.entity = entity;
		this.group = group;
	}

	public EntityWithLabel getEntity()
	{
		return entity;
	}

	public Group getGroup()
	{
		return group;
	}
}
