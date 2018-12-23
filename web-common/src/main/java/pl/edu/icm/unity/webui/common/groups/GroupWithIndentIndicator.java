/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */


package pl.edu.icm.unity.webui.common.groups;

import pl.edu.icm.unity.types.basic.Group;
/**
 * Helper class for creating mandatory group selection
 * @author P.Piernik
 *
 */
public class GroupWithIndentIndicator
{
	public final boolean indent;
	public final Group group;
	
	public GroupWithIndentIndicator(Group group, boolean indent)
	{
		this.group = group;
		this.indent = indent;
	}
}
