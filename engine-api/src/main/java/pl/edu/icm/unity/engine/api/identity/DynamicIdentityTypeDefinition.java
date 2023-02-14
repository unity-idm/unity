/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.identity;

public interface DynamicIdentityTypeDefinition extends IdentityTypeDefinition
{
	default boolean isDynamic()
	{
		return true;
	}

	default boolean isUserSettable()
	{
		return false;
	}
}

