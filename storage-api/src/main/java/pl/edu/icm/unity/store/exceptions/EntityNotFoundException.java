/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.exceptions;

public class EntityNotFoundException extends IllegalArgumentException
{
	public EntityNotFoundException(String s)
	{
		super(s);
	}
}
