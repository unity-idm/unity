/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.identities;

import pl.edu.icm.unity.types.basic.Entity;
import pl.edu.icm.unity.webui.bus.Event;

public class EntityChangedEvent implements Event
{
	private Entity entity;
	private String group;

	public EntityChangedEvent(Entity entity, String group)
	{
		this.entity = entity;
		this.group = group;
	}

	public Entity getEntity()
	{
		return entity;
	}

	public String getGroup()
	{
		return group;
	}
}
