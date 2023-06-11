/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.scim.schema;

import pl.edu.icm.unity.engine.api.exceptions.RuntimeEngineException;

public class UnsupportedAttributeTypeException extends RuntimeEngineException
{
	public UnsupportedAttributeTypeException(String msg)
	{
		super(msg);
	}

}
