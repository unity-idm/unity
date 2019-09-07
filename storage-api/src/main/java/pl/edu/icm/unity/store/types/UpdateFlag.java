/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.types;

/**
 * Flags are added to updates to provide additional context, so more fine grained consistency checking may be implemented 
 */
public enum UpdateFlag
{
	/**
	 * If this flag is present, then the update of type-like object won't make its instance invalid.
	 */
	DOESNT_MAKE_INSTANCES_INVALID
}
