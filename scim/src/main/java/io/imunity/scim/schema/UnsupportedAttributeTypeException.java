/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.scim.schema;

import pl.edu.icm.unity.exceptions.EngineException;

public class UnsupportedAttributeTypeException extends EngineException
{

	public UnsupportedAttributeTypeException(String msg)
	{
		super(msg);
	}

}
