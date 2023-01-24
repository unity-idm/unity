/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.api.group;

public class GroupNotFoundException extends RuntimeException
{
	public GroupNotFoundException(String message)
	{
		super(message);
	}
}
