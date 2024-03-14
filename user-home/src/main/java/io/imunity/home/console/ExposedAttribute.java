/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */


package io.imunity.home.console;

import io.imunity.vaadin.auth.services.idp.GroupWithIndentIndicator;
import pl.edu.icm.unity.base.group.Group;

public class ExposedAttribute
{
	private String name;
	private GroupWithIndentIndicator group;
	private boolean editable;
	private boolean showGroup;

	public ExposedAttribute()
	{
		group = new GroupWithIndentIndicator(new Group("/"), false);
	}

	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public GroupWithIndentIndicator getGroup()
	{
		return group;
	}

	public void setGroup(GroupWithIndentIndicator group)
	{
		this.group = group;
	}

	public boolean isEditable()
	{
		return editable;
	}

	public void setEditable(boolean editable)
	{
		this.editable = editable;
	}

	public boolean isShowGroup()
	{
		return showGroup;
	}

	public void setShowGroup(boolean showGroup)
	{
		this.showGroup = showGroup;
	}
}