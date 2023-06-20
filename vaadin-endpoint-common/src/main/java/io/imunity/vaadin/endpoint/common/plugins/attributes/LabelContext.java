/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.endpoint.common.plugins.attributes;

public class LabelContext
{
	final String label;
	final boolean showGroup;
	final String groupPath;
	final String groupDisplayedName;

	public LabelContext(String label, boolean showGroup, String groupPath, String groupDisplayedName)
	{
		this.label = label;
		this.showGroup = showGroup;
		this.groupPath = groupPath;
		this.groupDisplayedName = groupDisplayedName;
	}

	public LabelContext(String label)
	{
		this(label, false, null, null);
	}

	boolean isGroupDisplayable()
	{
		return showGroup && !groupPath.equals("/");
	}

	String getLabel()
	{
		if (isGroupDisplayable())
			return label + " (" + groupDisplayedName + ")";
		return label;
	}
}
