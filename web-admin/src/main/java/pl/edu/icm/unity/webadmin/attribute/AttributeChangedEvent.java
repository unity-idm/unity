/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.attribute;

import pl.edu.icm.unity.webui.bus.Event;

/**
 * Sent when attribute is added/removed/modified in a group.
 * @author K. Benedyczak
 */
public class AttributeChangedEvent implements Event
{
	private String group;
	private String attributeName;

	public AttributeChangedEvent(String group, String attributeName)
	{
		super();
		this.group = group;
		this.attributeName = attributeName;
	}

	public String getGroup()
	{
		return group;
	}

	public String getAttributeName()
	{
		return attributeName;
	}
}
