/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.api.translation.form;

public class DynamicGroupParam
{
	public static final String DYN_GROUP_PFX = "DYN:";

	public final int index;

	public DynamicGroupParam(int index)
	{
		this.index = index;
	}
	
	public DynamicGroupParam(String value)
	{
		String groupWithNumber = value.substring(DYN_GROUP_PFX.length());
		String[] split = groupWithNumber.split(":", 2);
		this.index = Integer.valueOf(split[0]);
	}

	public static boolean isDynamicGroup(String group)
	{
		return group.startsWith(DYN_GROUP_PFX);
	}

	public String toSelectionRepresentation()
	{
		return DYN_GROUP_PFX + index;
	}
}
