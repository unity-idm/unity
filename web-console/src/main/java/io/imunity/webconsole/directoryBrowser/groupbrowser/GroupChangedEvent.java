/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.webconsole.directoryBrowser.groupbrowser;

import pl.edu.icm.unity.base.group.Group;
import pl.edu.icm.unity.webui.bus.Event;

public class GroupChangedEvent implements Event
{
	private Group group;

	public GroupChangedEvent(Group group)
	{
		this.group = group;
	}

	public Group getGroup()
	{
		return group;
	}
}
