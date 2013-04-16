/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.groupbrowser;

import pl.edu.icm.unity.webui.bus.Event;

public class GroupChangedEvent implements Event
{
	private String group;

	public GroupChangedEvent(String group)
	{
		this.group = group;
	}

	public String getGroup()
	{
		return group;
	}
}
